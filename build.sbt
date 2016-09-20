name := "akka-cqrs"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

val akka = "2.4.8"
val circeV = "0.4.1"

libraryDependencies ++= {
  Seq(
    // akka
    "com.typesafe.akka"          %%  "akka-actor"                 % akka,
    "com.typesafe.akka"          %%  "akka-persistence"           % akka,
    "com.typesafe.akka"          %%  "akka-http-experimental"     % akka,
    "com.typesafe.akka"          %%  "akka-stream"                % akka,
    "com.typesafe.akka"          %%  "akka-cluster"               % akka,
    "com.typesafe.akka"          %%  "akka-cluster-sharding"      % akka,
    "com.typesafe.akka"          %%  "akka-cluster-tools"         % akka,
    "com.typesafe.akka"          %%  "akka-contrib"               % akka,
    "com.typesafe.akka"          %%  "akka-cluster-metrics"       % akka,

    // logging
    "com.typesafe.akka"          %%  "akka-slf4j"                 % akka,
    "com.typesafe.scala-logging" %%  "scala-logging"              % "3.4.0",
    "ch.qos.logback"              %  "logback-classic"            % "1.1.7",

    // persistence
    "com.typesafe.akka"          %%  "akka-persistence-cassandra" % "0.17",
    "com.zaxxer"                  %  "HikariCP"                   % "2.4.7",
    "com.typesafe.slick"         %%  "slick"                      % "3.1.1",
    "org.postgresql"              %  "postgresql"                 % "9.4.1209",
    "org.flywaydb"                %  "flyway-core"                % "4.0.3",

    // json
    "de.heikoseeberger"          %%  "akka-http-circe"            % "1.9.0",
    "io.circe"                   %%  "circe-core"                 % circeV,
    "io.circe"                   %%  "circe-generic"              % circeV,
    "io.circe"                   %%  "circe-parser"               % circeV,

    // util
    "com.google.inject"           %  "guice"                      % "4.1.0",
    "net.codingwell"             %%  "scala-guice"                % "4.1.0",
    "joda-time"                   %  "joda-time"                  % "2.9.4",
    "org.joda"                    %  "joda-convert"               % "1.8.1",
    "com.github.tototoshi"       %%  "slick-joda-mapper"          % "2.2.0",

    // test
    "com.typesafe.akka"          %%  "akka-testkit"               % akka       % Test,
    "junit"                       %  "junit"                      % "4.12"     % Test,
    "org.mockito"                 %  "mockito-all"                % "1.10.19"  % Test,
    "org.scalatest"              %%  "scalatest"                  % "3.0.0"    % Test,
    "ru.yandex.qatools.embed"     %  "postgresql-embedded"        % "1.15"     % Test
  )
}