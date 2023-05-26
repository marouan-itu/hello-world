package data

import slick.jdbc.PostgresProfile.api._

/** Represents the "users" table in the database.
  *
  * @param tag
  *   the tag to use for the table
  */
class UserTable(tag: Tag) extends Table[User](tag, "users") {

  /** The user's ID column. */
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  /** The user's username column. */
  def username = column[String]("username", O.NotNull)

  /** The user's password column. */
  def password = column[String]("password", O.NotNull)

  /** The user's salt column. */
  def salt = column[String]("salt", O.NotNull)

  /** The user's email column. */
  def email = column[String]("email", O.NotNull)

  /** The mapping of the user's data to the table's columns. */
  def * =
    (
      id,
      username,
      password,
      salt,
      email
    ) <> ((User.apply _).tupled, User.unapply)

  /** A unique index on the username column. */
  def usernameIndex = index("idx_username", username, unique = true)

  /** A unique index on the email column. */
  def emailIndex = index("idx_email", email, unique = true)
}

/** The companion object for the UserTable class. */
object UserTable {

  /** The query for the "users" table. */
  val Users = TableQuery[UserTable]
}
