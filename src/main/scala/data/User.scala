package data

import org.apache.commons.validator.routines.EmailValidator
import scala.util.Try
import java.security.SecureRandom
import java.util.Base64
import io.github.nremond.SecureHash

sealed trait ValidationError
case object InvalidUsername extends ValidationError
case object InvalidPassword extends ValidationError
case object InvalidEmail extends ValidationError
case object HashingError extends ValidationError

case class User private (
    id: Int,
    username: String,
    password: String,
    email: String,
    salt: String
) {
  override def toString: String = s"User($id, $username, ****, $email, ****)"

  def verifyPassword(password: String): Boolean = {
    val hashedPassword = SecureHash.createHash(password + this.salt)
    this.password == hashedPassword
  }
}

object User {
  private val emailValidator = EmailValidator.getInstance()

  def isValidUsername(username: String): Boolean =
    username.nonEmpty && username.length >= 3

  def isValidPassword(password: String): Boolean =
    password.nonEmpty &&
      password.length >= 8 &&
      password.exists(_.isUpper) &&
      password.exists(_.isLower) &&
      password.exists(_.isDigit) &&
      password.exists(!_.isLetterOrDigit)

  def isValidEmail(email: String): Boolean =
    emailValidator.isValid(email)

  def create(
      id: Int,
      username: String,
      password: String,
      email: String
  ): Try[Either[ValidationError, User]] = Try {
    if (!isValidUsername(username)) {
      Left(InvalidUsername)
    } else if (!isValidPassword(password)) {
      Left(InvalidPassword)
    } else if (!isValidEmail(email)) {
      Left(InvalidEmail)
    } else {
      val salt = generateSalt()
      val hashedPassword = Try(SecureHash.createHash(password + salt))
      hashedPassword match {
        case scala.util.Success(hashed) =>
          Right(User(id, username, hashed, email, salt))
        case scala.util.Failure(_) => Left(HashingError)
      }
    }
  }

  private def generateSalt(): String = {
    val random = new SecureRandom()
    val bytes = new Array[Byte](64)
    random.nextBytes(bytes)
    Base64.getEncoder.encodeToString(bytes)
  }
}
