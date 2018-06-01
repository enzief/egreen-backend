import com.round.Dependencies._

lazy val root = (project in file("."))
  .enablePlugins(com.round.ProjectPlugin)
  .settings(
    name := "egreen-backend",
    libraryDependencies ++= Seq(
      Cats.core,
      Cats.effect,
      Circe.core,
      Circe.literal,
      Circe.parser,
      Http4s.blaze,
      Http4s.circe,
      Http4s.dsl,
      Scalatest.core % "test",
      Logback.classic
    )
  )

addCommandAlias(
  "fmt",
  ";scalafmtSbt;scalafmt;test:scalafmt"
)

// while you're working, try putting "~wip" into your sbt console
// ...but be prepared to let IntelliJ force you to reload!
addCommandAlias(
  "wip",
  ";fmt;test:compile"
)
