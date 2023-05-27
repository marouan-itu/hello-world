package data

import cats.effect.{IO, Resource}
import skunk.{Session, Command, Query, ~}
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import com.typesafe.scalalogging.LazyLogging
import io.github.nremond.SecureHash

class PostgresUserRepository(sessionPool: Resource[IO, Session[IO]])
    extends UserRepository
    with LazyLogging {

  import User._

  // Queries
  private val selectUser: Query[String, User] =
    sql"""
      SELECT id, username, password, salt, email
      FROM Users
      WHERE username = $varchar
    """.query(int4 ~ varchar ~ varchar ~ varchar ~ varchar).map(User.tupled)

  private val selectUserById: Query[Int, User] =
    sql"""
      SELECT id, username, password, salt, email
      FROM Users
      WHERE id = $int4
    """.query(int4 ~ varchar ~ varchar ~ varchar ~ varchar).map(User.tupled)

  private val selectAllUsers: Query[Void, User] =
    sql"""
      SELECT id, username, password, salt, email
      FROM Users
    """.query(int4 ~ varchar ~ varchar ~ varchar ~ varchar).map(User.tupled)

  private val usernameExists: Query[String, Boolean] =
    sql"""
      SELECT EXISTS (
        SELECT 1 FROM Users WHERE username = $varchar
      )
    """.query(bool)

  // Commands
  private val insertUser: Command[User] =
    sql"""
      INSERT INTO Users (username, password, salt, email)
      VALUES ($varchar, $varchar, $varchar, $varchar)
    """.command.contramap(User.unapply(_).get)

  private val updateUser: Command[User] =
    sql"""
      UPDATE Users
      SET username = $varchar, password = $varchar, salt = $varchar, email = $varchar
      WHERE id = $int4
    """.command.contramap(User.unapply(_).get)

  private val deleteUser: Command[Int] =
    sql"""
      DELETE FROM Users
      WHERE id = $int4
    """.command

  override def createUser(user: User): IO[Either[RepositoryError, User]] = {
    sessionPool.use { session =>
      session.prepare(usernameExists).use { query =>
        query.unique(user.username).flatMap { exists =>
          if (exists) {
            IO.pure(Left(UsernameAlreadyExistsError))
          } else {
            session.prepare(insertUser).use { cmd =>
              cmd.execute(user).attempt.map {
                case Left(e) =>
                  logger.error(s"Error creating a user: ${e.getMessage}", e)
                  Left(DatabaseError(e))
                case Right(_) =>
                  logger.info(s"Successfully created user: ${user.username}")
                  Right(user)
              }
            }
          }
        }
      }
    }
  }

  override def getUser(id: Int): IO[Either[RepositoryError, Option[User]]] = {
    sessionPool.use { session =>
      session.prepare(selectUserById).use { query =>
        query.option(id).attempt.map {
          case Left(e) =>
            logger.error(s"Error retrieving a user: ${e.getMessage}", e)
            Left(DatabaseError(e))
          case Right(userOption) =>
            logger.info(s"Successfully retrieved user: $id")
            Right(userOption)
        }
      }
    }
  }

  override def updateUser(user: User): IO[Either[RepositoryError, User]] = {
    sessionPool.use { session =>
      session.prepare(updateUser).use { cmd =>
        cmd.execute(user).attempt.map {
          case Left(e) =>
            logger.error(s"Error updating a user: ${e.getMessage}", e)
            Left(DatabaseError(e))
          case Right(_) =>
            logger.info(s"Successfully updated user: ${user.username}")
            Right(user)
        }
      }
    }
  }

  override def deleteUser(id: Int): IO[Either[RepositoryError, Unit]] = {
    sessionPool.use { session =>
      session.prepare(selectUserById).use { query =>
        query.option(id).flatMap { userOption =>
          userOption match {
            case Some(_) =>
              session.prepare(deleteUser).use { cmd =>
                cmd.execute(id).attempt.map {
                  case Left(e) =>
                    logger.error(s"Error deleting a user: ${e.getMessage}", e)
                    Left(DatabaseError(e))
                  case Right(_) =>
                    logger.info(s"Successfully deleted user: $id")
                    Right(())
                }
              }
            case None => IO.pure(Left(UserNotFoundError))
          }
        }
      }
    }
  }

  override def getUserByUsername(username: String): IO[Either[RepositoryError, Option[User]]] = {
    sessionPool.use { session =>
      session.prepare(selectUser).use { query =>
        query.option(username).attempt.map {
          case Left(e) =>
            logger.error(s"Error retrieving a user by username: ${e.getMessage}", e)
            Left(DatabaseError(e))
          case Right(userOption) =>
            logger.info(s"Successfully retrieved user by username: $username")
            Right(userOption)
        }
      }
    }
  }

  override def isUsernameTaken(username: String): IO[Either[RepositoryError, Boolean]] = {
    sessionPool.use { session =>
      session.prepare(usernameExists).use { query =>
        query.unique(username).attempt.map {
          case Left(e) =>
            logger.error(s"Error checking if a username is taken: ${e.getMessage}", e)
            Left(DatabaseError(e))
          case Right(isTaken) =>
            logger.info(s"Username check complete for: $username")
            Right(isTaken)
        }
      }
    }
  }

  override def getAllUsers: IO[Either[RepositoryError, Seq[User]]] = {
    sessionPool.use { session =>
      session.prepare(selectAllUsers).use { query =>
        query.stream(Void, 64).compile.toList.attempt.map {
          case Left(e) =>
            logger.error(s"Error retrieving all users: ${e.getMessage}", e)
            Left(DatabaseError(e))
          case Right(users) =>
            logger.info(s"Successfully retrieved all users")
            Right(users)
        }
      }
    }
  }

  override def verifyPassword(username: String, password: String): IO[Either[RepositoryError, User]] = {
    sessionPool.use { session =>
      session.prepare(selectUser).use { query =>
        query.option(username).map {
          case Some(user) =>
            if (user.password == SecureHash.createHash(password, user.salt)) {
              Right(user)
            } else {
              Left(PasswordIncorrectError)
            }
          case None => Left(UserNotFoundError)
        }
      }
    }
  }
}
