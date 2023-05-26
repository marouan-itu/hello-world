package data

import org.apache.commons.validator.routines.EmailValidator

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
  * @param salt
  *   the user's salt for password hashing (private)
  * @param email
  *   the user's email
  */
class User private (
    val id: Int,
    val username: String,
    private val password: String,
    private val salt: String,
    val email: String
) {
  override def toString: String = s"User($id, $username, ****, ****, $email)"
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
  private[data] def isValidUsername(username: String): Boolean =
    username.nonEmpty && username.length >= 3

  /** Checks if a password is valid.
    *
    * @param password
    *   the password to check
    * @return
    *   true if the password is valid, false otherwise
    */
  private[data] def isValidPassword(password: String): Boolean =
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
  private[data] def isValidEmail(email: String): Boolean =
    emailValidator.isValid(email)
    
  /** Creates a new user.
    *
    * @param id
    *   the user's ID
    * @param username
    *   the user's username
    * @param password
    *   the user's password
    * @param salt
    *   the user's salt for password hashing
    * @param email
    *   the user's email
    * @return
    *   the created user, or a validation error if the user's data is invalid
    */
  def create(
      id: Int,
      username: String,
      password: String,
      salt: String,
      email: String
  ): Either[ValidationError, User] = {
    if (!isValidUsername(username)) {
      Left(InvalidUsername)
    } else if (!isValidPassword(password)) {
      Left(InvalidPassword)
    } else if (!isValidEmail(email)) {
      Left(InvalidEmail)
    } else {
      Right(new User(id, username, password, salt, email))
    }
  }
}
