package data

import org.apache.commons.validator.routines.EmailValidator
import scala.concurrent.Future
import io.github.nremond.SecureHash
import java.security.SecureRandom

/** Represents a validation error for a user's data. */
sealed trait ValidationError
case object InvalidUsername extends ValidationError
case object InvalidPassword extends ValidationError
case object InvalidEmail extends ValidationError

/** Represents a user.
  *
  * @param id
  *   the user's ID
  * @param username
  *   the user's username
  * @param password
  *   the user's password (private)
  * @param email
  *   the user's email
  */
class User private (
    val id: Int,
    val username: String,
    private val password: String,
    val email: String
) {
  override def toString: String = s"User($id, $username, ****, $email)"

  /** Verifies if the provided password matches the user's password.
    *
    * @param password
    *   the password to verify
    * @return
    *   true if the password is correct, false otherwise
    */
  def verifyPassword(password: String): Boolean =
    SecureHash.validatePassword(password, this.password)
}

object User {
  private val emailValidator = EmailValidator.getInstance()

  /** Checks if a username is valid.
    *
    * @param username
    *   the username to check
    * @return
    *   true if the username is valid, false otherwise
    */
  def isValidUsername(username: String): Boolean =
    username.nonEmpty && username.length >= 3

  /** Checks if a password is valid.
    *
    * @param password
    *   the password to check
    * @return
    *   true if the password is valid, false otherwise
    */
  def isValidPassword(password: String): Boolean =
    password.nonEmpty &&
      password.length >= 8 &&
      password.exists(_.isUpper) &&
      password.exists(_.isLower) &&
      password.exists(_.isDigit) &&
      password.exists(!_.isLetterOrDigit)

  /** Checks if an email is valid.
    *
    * @param email
    *   the email to check
    * @return
    *   true if the email is valid, false otherwise
    */
  def isValidEmail(email: String): Boolean =
    emailValidator.isValid(email)

  /** Creates a new user.
    *
    * @param id
    *   the user's ID
    * @param username
    *   the user's username
    * @param password
    *   the user's password
    * @param email
    *   the user's email
    * @return
    *   a Future that either contains the created user, or a validation error if
    *   the user's data is invalid
    */
  def create(
      id: Int,
      username: String,
      password: String,
      email: String
  ): Future[Either[ValidationError, User]] = Future {
    if (!isValidUsername(username)) {
      Left(InvalidUsername)
    } else if (!isValidPassword(password)) {
      Left(InvalidPassword)
    } else if (!isValidEmail(email)) {
      Left(InvalidEmail)
    } else {
      val salt = generateSalt()
      val hashedPassword = SecureHash.createHash(password, salt)
      Right(new User(id, username, hashedPassword, email))
    }
  }

  private def generateSalt(): String = {
    val random = new SecureRandom()
    val bytes = new Array[Byte](64)
    random.nextBytes(bytes)
    bytes.map("%02x".format(_)).mkString
  }
}
