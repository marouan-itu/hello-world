import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import data._

class UserSpec extends FlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Custom generators for valid and invalid usernames, passwords, and emails
  val validUsername: Gen[String] =
    Gen.alphaStr.suchThat(s => s.length >= 3 && s.length <= 20 && s.forall(_.isLetterOrDigit))
  val invalidUsername: Gen[String] =
    Gen.oneOf(Gen.alphaStr.suchThat(s => s.length < 3 || s.length > 20 || s.contains(" ")), Gen.const(""))

  val validPassword: Gen[String] = for {
    upper <- Gen.alphaUpperChar
    lower <- Gen.alphaLowerChar
    digit <- Gen.numChar
    nonAlphaNum <- Gen.oneOf(
      Gen.oneOf('!', '@', '#', '$', '%', '^', '&', '*'),
      Gen.oneOf('~', '(', ')', '-', '_', '=', '+')
    )
    rest <- Gen.alphaStr.suchThat(_.length >= 4 && _.length <= 20)
  } yield upper.toString + lower.toString + digit.toString + nonAlphaNum.toString + rest
  val invalidPassword: Gen[String] =
    Gen.oneOf(Gen.alphaStr.suchThat(s => s.length < 8 || s.length > 20 || s.forall(_.isLetterOrDigit)), Gen.const(""))

  val validEmail: Gen[String] = for {
    local <- Gen.alphaStr.suchThat(_.forall(_.isLetterOrDigit))
    domain <- Gen.oneOf("example.com", "test.org", "mywebsite.net")
  } yield local + "@" + domain
  val invalidEmail: Gen[String] =
    Gen.oneOf(Gen.alphaStr.suchThat(s => !s.contains("@") || !s.contains(".") || s.contains(" ")), Gen.const(""))

  "User.create" should "return a User for valid input" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) match {
        case Right(user) =>
          user.id should be(id)
          user.username should be(username)
          user.email should be(email)
          // Check if validatePassword method exists in User class
          assert(user.getClass.getMethods.map(_.getName).contains("validatePassword"))
          // Assuming we have a method to validate the password with the salt
          assert(user.validatePassword(password, salt).isRight)
        case Left(error) =>
          fail(
            s"Expected Right(User) but got Left($error) for input: id=$id, username=$username, password=$password, salt=$salt, email=$email"
          )
      }
    }
  }

  it should "return InvalidUsername when username is invalid" in {
    forAll(
      Gen.posNum[Int],
      invalidUsername,
      validPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be(
        Left(InvalidUsername)
      )
    }
  }

  it should "return InvalidPassword when password is invalid" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      invalidPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be(
        Left(InvalidPassword)
      )
    }
  }

  it should "return InvalidEmail when email is invalid" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.alphaStr,
      invalidEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be(
        Left(InvalidEmail)
      )
    }
  }

  it should "return InvalidUsername when username is empty" in {
    forAll(
      Gen.posNum[Int],
      Gen.const(""),
      validPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be(
        Left(InvalidUsername)
      )
    }
  }

  it should "return InvalidPassword when password is empty" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      Gen.const(""),
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be(
        Left(InvalidPassword)
      )
    }
  }

  it should "return InvalidEmail when email is empty" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.alphaStr,
      Gen.const("")
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be(
        Left(InvalidEmail)
      )
    }
  }

  // Additional test cases
  it should "return UserAlreadyExists when user with the same ID already exists" in {
    // Assuming we have a method to check if a user already exists
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email)
      User.create(id, username, password, salt, email) should be(
        Left(UserAlreadyExists)
      )
    }
  }

  it should "return an exception when an error occurs during user creation" in {
    // Assuming we have a method to simulate an error during user creation
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      User.create(id, username, password, salt, email) should be a 'failure
    }
  }

  it should "handle concurrent user creation" in {
    // Assuming we have a method to simulate concurrent user creation
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.alphaStr,
      validEmail
    ) { (id, username, password, salt, email) =>
      val userCreation1 = Future(User.create(id, username, password, salt, email))
      val userCreation2 = Future(User.create(id, username, password, salt, email))
      Await.result(userCreation1, 3.seconds) should not be Await.result(userCreation2, 3.seconds)
    }
  }
}
