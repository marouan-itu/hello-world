import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import data._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

class UserSpec extends FlatSpec with Matchers with ScalaCheckPropertyChecks with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

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
      validEmail
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) match {
        case scala.util.Success(Right(user)) =>
          user.id should be(id)
          user.username should be(username)
          user.email should be(email)
        case scala.util.Success(Left(error)) =>
          fail(
            s"Expected Right(User) but got Left($error) for input: id=$id, username=$username, password=$password, email=$email"
          )
        case scala.util.Failure(_) =>
          fail("Hashing error occurred during user creation")
      }
    }
  }

  it should "return InvalidUsername when username is invalid" in {
    forAll(
      Gen.posNum[Int],
      invalidUsername,
      validPassword,
      validEmail
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) should be(
        scala.util.Success(Left(InvalidUsername))
      )
    }
  }

  it should "return InvalidPassword when password is invalid" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      invalidPassword,
      validEmail
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) should be(
        scala.util.Success(Left(InvalidPassword))
      )
    }
  }

  it should "return InvalidEmail when email is invalid" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      invalidEmail
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) should be(
        scala.util.Success(Left(InvalidEmail))
      )
    }
  }

  it should "return InvalidUsername when username is empty" in {
    forAll(
      Gen.posNum[Int],
      Gen.const(""),
      validPassword,
      validEmail
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) should be(
        scala.util.Success(Left(InvalidUsername))
      )
    }
  }

  it should "return InvalidPassword when password is empty" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      Gen.const(""),
      validEmail
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) should be(
        scala.util.Success(Left(InvalidPassword))
      )
    }
  }

  it should "return InvalidEmail when email is empty" in {
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      Gen.const("")
    ) { (id, username, password, email) =>
      User.create(id, username, password, email) should be(
        scala.util.Success(Left(InvalidEmail))
      )
    }
  }

  it should "return HashingError when an error occurs during user creation" in {
    // Assuming we have a method to simulate an error during user creation
    forAll(
      Gen.posNum[Int],
      validUsername,
      validPassword,
      validEmail
    ) { (id, username, password, email) =>
      // Simulate a hashing error
      when(SecureHash.createHash(any[String])).thenThrow(new RuntimeException("Simulated hashing error"))
      User.create(id, username, password, email) should be a 'failure
    }
  }
}
