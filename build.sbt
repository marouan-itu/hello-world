val scala3Version = "3.2.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "hello-world",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "org.tpolecat" %% "skunk-core" % "0.6.0-RC2",
      "io.github.nremond" %% "pbkdf2-scala" % "0.7.0",
      "org.postgresql" % "postgresql" % "42.5.0",
      "commons-validator" % "commons-validator" % "1.7",
      "org.scalactic" %% "scalactic" % "3.2.16",
      "org.scalatest" %% "scalatest" % "3.2.16" % "test",
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0" % "test",
      "org.typelevel" %% "cats-effect" % "3.5.0",
      "org.typelevel" %% "cats-core" % "2.9.0"
      )
  )

  //      "io.github.nremond" %% "pbkdf2-scala" % "0.7.0",
