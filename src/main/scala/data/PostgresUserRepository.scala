package data

import cats.effect.{IO, Resource}
import cats.data.EitherT
import skunk.{Session, Command, Query, ~}
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import com.typesafe.scalalogging.LazyLogging
import io.github.nremond.SecureHash
import java.security.MessageDigest
import java.nio.charset.StandardCharsets

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
    logger.debug(s"Attempting to create user: ${user.username}")
    val action = for {
      result <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(insertUser).use { cmd =>
            cmd.execute(user)
          }
        })
        .leftMap {
          case e: skunk.exception.PostgresErrorException
              if e.message.contains("unique constraint") =>
            UsernameAlreadyExistsError
          case e => DatabaseError(e)
        }
    } yield result

    action.value
  }

  override def getUser(id: Int): IO[Either[RepositoryError, Option[User]]] = {
    logger.debug(s"Attempting to retrieve user with id: $id")
    val action = for {
      userOption <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(selectUserById).use { query =>
            query.option(id)
          }
        })
        .leftMap(e => DatabaseError(e))
    } yield userOption

    action.value
  }

  override def updateUser(user: User): IO[Either[RepositoryError, User]] = {
    logger.debug(s"Attempting to update user: ${user.username}")
    val action = for {
      result <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(updateUser).use { cmd =>
            cmd.execute(user)
          }
        })
        .leftMap {
          case e: skunk.exception.PostgresErrorException
              if e.message.contains("does not exist") =>
            UserNotFoundError
          case e => DatabaseError(e)
        }
    } yield result

    action.value
  }

  override def deleteUser(id: Int): IO[Either[RepositoryError, Unit]] = {
    logger.debug(s"Attempting to delete user with id: $id")
    val action = for {
      result <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(deleteUser).use { cmd =>
            cmd.execute(id)
          }
        })
        .leftMap {
          case e: skunk.exception.PostgresErrorException
              if e.message.contains("does not exist") =>
            UserNotFoundError
          case e => DatabaseError(e)
        }
    } yield result

    action.value
  }

  override def getUserByUsername(
      username: String
  ): IO[Either[RepositoryError, Option[User]]] = {
    logger.debug(s"Attempting to retrieve user by username: $username")
    val action = for {
      userOption <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(selectUser).use { query =>
            query.option(username)
          }
        })
        .leftMap(e => DatabaseError(e))
    } yield userOption

    action.value
  }

  override def isUsernameTaken(
      username: String
  ): IO[Either[RepositoryError, Boolean]] = {
    logger.debug(s"Checking if username is taken: $username")
    val action = for {
      isTaken <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(usernameExists).use { query =>
            query.unique(username)
          }
        })
        .leftMap(e => DatabaseError(e))
    } yield isTaken

    action.value
  }

  override def getAllUsers: IO[Either[RepositoryError, Seq[User]]] = {
    logger.debug("Attempting to retrieve all users")
    val action = for {
      users <- EitherT
        .right(sessionPool.use { session =>
          session.prepare(selectAllUsers).use { query =>
            query.stream(Void, 64).compile.toList
          }
        })
        .leftMap(e => DatabaseError(e))
    } yield users

    action.value
  }

  override def verifyPassword(
      username: String,
      password: String
  ): IO[Either[RepositoryError, User]] = {
    sessionPool
      .use { session =>
        session.prepare(selectUser).use { query =>
          query.option(username).map {
            case Some(user) =>
              val hashedPassword = SecureHash.createHash(password + user.salt)
              if (
                MessageDigest.isEqual(
                  hashedPassword.getBytes(StandardCharsets.UTF_8),
                  user.password.getBytes(StandardCharsets.UTF_8)
                )
              ) {
                Right(user)
              } else {
                Left(PasswordIncorrectError)
              }
            case None => Left(UserNotFoundError)
          }
        }
      }
      .handleErrorWith { case e: Throwable =>
        IO.pure(Left(DatabaseError(e)))
      }
  }
}
