package data

import scala.concurrent.{ExecutionContext, Future}
import com.github.t3hnar.bcrypt._
import slick.jdbc.PostgresProfile.api._
import com.typesafe.scalalogging.LazyLogging

/** A repository for performing operations on users using a PostgreSQL database.
  *
  * @param db
  *   the database connection
  * @param ec
  *   the execution context for asynchronous operations
  */
class PostgresUserRepository(db: Database)(implicit ec: ExecutionContext)
    extends UserRepository
    with LazyLogging {
  import UserTable._

  /** Hashes a password with a salt using bcrypt.
    *
    * @param password
    *   the password to hash
    * @param salt
    *   the salt to use for hashing
    * @return
    *   the hashed password
    */
  private def hashPassword(password: String, salt: String): String =
    (password + salt).bcryptBounded

  /** Creates a new user.
    *
    * @param user
    *   the user to create
    * @return
    *   a future that completes with the created user or an error
    */
  override def createUser(user: User): Future[Either[RepositoryError, User]] = {
    // Implementation omitted for brevity
  }

  /** Retrieves a user by ID.
    *
    * @param id
    *   the ID of the user to retrieve
    * @return
    *   a future that completes with the user or an error
    */
  override def getUser(
      id: Int
  ): Future[Either[RepositoryError, Option[User]]] = {
    // Implementation omitted for brevity
  }

  /** Updates a user.
    *
    * @param user
    *   the user with updated information
    * @return
    *   a future that completes with the updated user or an error
    */
  override def updateUser(user: User): Future[Either[RepositoryError, User]] = {
    // Implementation omitted for brevity
  }

  /** Deletes a user by ID.
    *
    * @param id
    *   the ID of the user to delete
    * @return
    *   a future that completes when the user has been deleted or an error
    */
  override def deleteUser(id: Int): Future[Either[RepositoryError, Unit]] = {
    // Implementation omitted for brevity
  }

  /** Retrieves a user by username.
    *
    * @param username
    *   the username of the user to retrieve
    * @return
    *   a future that completes with the user or an error
    */
  override def getUserByUsername(
      username: String
  ): Future[Either[RepositoryError, Option[User]]] = {
    // Implementation omitted for brevity
  }

  /** Checks if a username is already taken.
    *
    * @param username
    *   the username to check
    * @return
    *   a future that completes with true if the username is taken, false
    *   otherwise, or an error
    */
  override def isUsernameTaken(
      username: String
  ): Future[Either[RepositoryError, Boolean]] = {
    // Implementation omitted for brevity
  }

  /** Retrieves all users.
    *
    * @return
    *   a future that completes with a sequence of all users or an error
    */
  override def getAllUsers: Future[Either[RepositoryError, Seq[User]]] = {
    // Implementation omitted for brevity
  }

  /** Handles exceptions and logs errors for a future.
    *
    * @param action
    *   a description of the action being performed
    * @param future
    *   the future to handle exceptions for
    * @return
    *   a future that completes with the result of the original future or an
    *   error
    */
  private def recoverWithLogging[T](
      action: String
  )(future: Future[T]): Future[Either[RepositoryError, T]] = {
    future.map(Right(_)).recover {
      case ex: java.sql.SQLException =>
        logger.error(
          s"A SQL error occurred while $action: ${ex.getMessage}",
          ex
        )
        Left(DatabaseError(ex))
      case ex: Exception =>
        logger.error(s"An error occurred while $action: ${ex.getMessage}", ex)
        Left(UnknownError(ex))
    }
  }
}
