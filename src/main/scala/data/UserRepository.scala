package data

import cats.data.EitherT
import cats.effect.IO

trait UserRepository {

  def createUser(
      username: String,
      password: String,
      email: String
  ): EitherT[IO, RepositoryError, User]

  def getUser(id: Int): EitherT[IO, RepositoryError, Option[User]]

  def getUserByUsername(
      username: String
  ): EitherT[IO, RepositoryError, Option[User]]

  def isUsernameTaken(
      username: String
  ): EitherT[IO, RepositoryError, Boolean]

  def getAllUsers(
      page: Int,
      pageSize: Int
  ): EitherT[IO, RepositoryError, Seq[User]]

  def updateUser(
      id: Int,
      username: Option[String],
      password: Option[String],
      email: Option[String]
  ): EitherT[IO, RepositoryError, User]

  def deleteUser(id: Int): EitherT[IO, RepositoryError, Unit]

  def changeUserPassword(
      id: Int,
      oldPassword: String,
      newPassword: String
  ): EitherT[IO, RepositoryError, Unit]

  def verifyUserCredentials(
      username: String,
      password: String
  ): EitherT[IO, RepositoryError, User]
}

sealed trait RepositoryError extends Throwable

case class DatabaseError(message: String) extends RepositoryError

case object UserNotFound extends RepositoryError

case object UsernameTaken extends RepositoryError

case object EmailTaken extends RepositoryError

case object InvalidCredentials extends RepositoryError

case object NoUpdateParametersProvided extends RepositoryError
