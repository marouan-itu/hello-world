package data

import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.IO
import cats.syntax.all._

class UserTable {

  // Define the codec for the User case class
  def userCodec: Codec[User] =
    (int4 ~ varchar ~ varchar ~ varchar ~ varchar).imap {
      case id ~ username ~ password ~ salt ~ email =>
        User(id, username, password, salt, email)
    } { case User(id, username, password, salt, email) =>
      id ~ username ~ password ~ salt ~ email
    }

  // Define the SQL commands
  def createUser: Command[User] =
    sql"""
      INSERT INTO users (id, username, password, salt, email)
      VALUES (DEFAULT, $varchar, $varchar, $varchar, $varchar)
      RETURNING id, username, password, salt, email
    """.command.gcontramap(userCodec)

  def getUser: Query[Int, User] =
    sql"""
      SELECT id, username, password, salt, email
      FROM users
      WHERE id = $int4
    """.query(userCodec)

  def updateUser: Command[User] =
    sql"""
      UPDATE users
      SET username = $varchar, password = $varchar, salt = $varchar, email = $varchar
      WHERE id = $int4
      RETURNING id, username, password, salt, email
    """.command.gcontramap(userCodec)

  def deleteUser: Command[Int] =
    sql"""
      DELETE FROM users
      WHERE id = $int4
    """.command

  def getAllUsers: Query[Void, User] =
    sql"""
      SELECT id, username, password, salt, email
      FROM users
    """.query(userCodec)

  // Error handling
  def execute[A](io: IO[A]): IO[Either[String, A]] =
    io.attempt.map {
      case Left(e)  => Left(e.getMessage)
      case Right(a) => Right(a)
    }
}
