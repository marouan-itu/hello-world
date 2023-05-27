package data

import scala.concurrent.{ExecutionContext, Future}
import io.github.nremond.SecureHash
import slick.jdbc.PostgresProfile.api._
import com.typesafe.scalalogging.LazyLogging
import slick.SlickException

class PostgresUserRepository(db: Database)(implicit ec: ExecutionContext)
    extends UserRepository
    with LazyLogging {
  import UserTable._

  private def runDbAction[T](
      action: DBIO[T],
      logMessage: String,
      logParams: String = ""
  ): Future[Either[RepositoryError, T]] =
    db.run(action)
      .map { result =>
        logger.info(
          s"Successfully completed $logMessage. Parameters: $logParams"
        )
        Right(result)
      }
      .recover {
        case ex: java.sql.SQLException =>
          logger.error(
            s"A SQL error occurred while $logMessage: ${ex.getMessage}",
            ex
          )
          Left(DatabaseError(ex))
        case ex: java.util.concurrent.TimeoutException =>
          logger.error(
            s"A timeout occurred while $logMessage: ${ex.getMessage}",
            ex
          )
          Left(TimeoutError(ex))
        case ex: SlickException =>
          logger.error(
            s"A database error occurred while $logMessage: ${ex.getMessage}",
            ex
          )
          Left(DatabaseError(ex))
        case ex: Exception =>
          logger.error(
            s"An error occurred while $logMessage: ${ex.getMessage}",
            ex
          )
          Left(UnknownError(ex))
      }

  override def createUser(user: User): Future[Either[RepositoryError, User]] = {
    val action = (for {
      isTaken <- Users.filter(_.username === user.username).exists.result
      result <-
        if (isTaken) {
          DBIO.successful(Left(UsernameAlreadyExistsError))
        } else {
          (Users returning Users.map(_.id) += user).map(id =>
            Right(user.copy(id = id))
          )
        }
    } yield result).transactionally

    runDbAction(action, "creating a user", s"Username: ${user.username}")
  }

  override def getUser(id: Int): Future[Either[RepositoryError, Option[User]]] =
    runDbAction(
      Users.filter(_.id === id).result.headOption,
      "retrieving a user",
      s"User ID: $id"
    )

  override def updateUser(user: User): Future[Either[RepositoryError, User]] = {
    val query = Users
      .filter(_.id === user.id)
      .map(u => (u.username, u.password, u.salt, u.email))
      .update((user.username, user.password, user.salt, user.email))

    runDbAction(query, "updating a user", s"User ID: ${user.id}")
  }

  override def deleteUser(id: Int): Future[Either[RepositoryError, Unit]] = {
    val action = (for {
      userExists <- Users.filter(_.id === id).exists.result
      result <-
        if (!userExists) {
          DBIO.successful(Left(UserNotFoundError))
        } else {
          Users.filter(_.id === id).delete.map(_ => Right(()))
        }
    } yield result).transactionally

    runDbAction(action, "deleting a user", s"User ID: $id")
  }

  override def getUserByUsername(
      username: String
  ): Future[Either[RepositoryError, Option[User]]] =
    runDbAction(
      Users.filter(_.username === username).result.headOption,
      "retrieving a user by username",
      s"Username: $username"
    )

  override def isUsernameTaken(
      username: String
  ): Future[Either[RepositoryError, Boolean]] =
    runDbAction(
      Users.filter(_.username === username).exists.result,
      "checking if a username is taken",
      s"Username: $username"
    )

  override def getAllUsers: Future[Either[RepositoryError, Seq[User]]] =
    runDbAction(Users.result, "retrieving all users")

  def verifyPassword(
      username: String,
      password: String
  ): Future[Either[RepositoryError, User]] = {
    val action = (for {
      userOption <- Users.filter(_.username === username).result.headOption
      result <- userOption match {
        case Some(user) =>
          if (user.password == SecureHash.createHash(password, user.salt)) {
            DBIO.successful(Right(user))
          } else {
            DBIO.successful(Left(PasswordIncorrectError))
          }
        case None => DBIO.successful(Left(UserNotFoundError))
      }
    } yield result).transactionally

    runDbAction(action, "verifying a password", s"Username: $username")
  }
}
