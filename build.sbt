val scala3Version = "3.2.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "hello-world",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "io.github.nremond" %% "pbkdf2-scala" % "0.7.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "org.tpolecat" %% "skunk-core" % "0.6.0-RC2",
      "org.postgresql" % "postgresql" % "42.5.0",
      "commons-validator" % "commons-validator" % "1.7",
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test,
      "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.2.9.0" % Test,
      "org.scalacheck" %% "scalacheck" % "1.15.4" % Test
    )
  )
