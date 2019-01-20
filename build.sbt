name := "web-project-backend"

version := "0.1"

scalaVersion := "2.12.8"

val circeVersion = "0.10.0"
val akkaHttpVersion = "10.1.5"
val akkaVersion = "2.5.18"
val doobieVersion = "0.6.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
)

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion
)

libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.10.1"

libraryDependencies += "com.microsoft.sqlserver" % "mssql-jdbc" % "7.0.0.jre10"