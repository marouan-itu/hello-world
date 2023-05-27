package data

import scala.concurrent.Future

/** Repository for performing operations on users.
  */
trait UserRepository {

  /** A type alias for a Future that either contains a value of type T or a
    * RepositoryError.
    */
  type Result[T] = Future[Either[RepositoryError, T]]

  /** Creates a new user.
    *
    * @param user
    *   The user to create.
    * @return
    *   A Future that either contains the created user with the ID set, or a
    *   RepositoryError if the operation failed.
    */
  def createUser(user: User): Result[User]

  /** Retrieves a user by ID.
    *
    * @param id
    *   The ID of the user to retrieve.
    * @return
    *   A Future that either contains the user if found, None if not found, or a
    *   RepositoryError if the operation failed.
    */
  def getUser(id: Int): Result[Option[User]]

  /** Retrieves a user by username.
    *
    * @param username
    *   The username of the user to retrieve.
    * @return
    *   A Future that either contains the user if found, None if not found, or a
    *   RepositoryError if the operation failed.
    */
  def getUserByUsername(username: String): Result[Option[User]]

  /** Checks if a username is already taken.
    *
    * @param username
    *   The username to check.
    * @return
    *   A Future that either contains true if the username is taken, false if
    *   not, or a RepositoryError if the operation failed.
    */
  def isUsernameTaken(username: String): Result[Boolean]

  /** Retrieves all users.
    *
    * @return
    *   A Future that either contains a sequence of all users, or a
    *   RepositoryError if the operation failed.
    */
  def getAllUsers: Result[Seq[User]]

  /** Updates a user.
    *
    * @param user
    *   The user with updated information.
    * @return
    *   A Future that either contains the updated user, or a RepositoryError if
    *   the operation failed.
    */
  def updateUser(user: User): Result[User]

  /** Deletes a user by ID.
    *
    * @param id
    *   The ID of the user to delete.
    * @return
    *   A Future that completes when the user has been deleted, or a
    *   RepositoryError if the operation failed.
    */
  def deleteUser(id: Int): Result[Unit]
}

/** A sealed trait representing possible errors that can occur in the
  * repository.
  */
sealed trait RepositoryError

/** A case class representing a database error. Contains a message describing
  * the error.
  */
case class DatabaseError(message: String) extends RepositoryError

/** A case object representing an error where a user was not found. */
case object UserNotFound extends RepositoryError

/** A case object representing an error where a username is already taken. */
case object UsernameTaken extends RepositoryError

/** A case object representing an error where an email is already taken. */
case object EmailTaken extends RepositoryError
