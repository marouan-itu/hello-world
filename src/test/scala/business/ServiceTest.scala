class ServiceTest extends AnyFunSuite {
  test("dummy test") {
    val result = dummyMethod()
    assert(result == "dummy")
  }

  def dummyMethod(): String = {
    "dummy"
  }
}