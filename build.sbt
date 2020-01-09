import com.typesafe.sbt.packager.docker._

scalaVersion := "2.13.1"
scalacOptions ++= Seq("-deprecation", "-feature", "-language:postfixOps")

enablePlugins(JavaServerAppPackaging)

libraryDependencies ++= {
  val management = "1.0.5"
  val akka       = "2.6.1"
  val http       = "10.1.11"
  Seq(
    "com.typesafe.akka"             %% "akka-actor-typed"                  % akka,
    "com.typesafe.akka"             %% "akka-cluster-typed"                % akka,
    "com.typesafe.akka"             %% "akka-persistence"                  % akka,
    "com.typesafe.akka"             %% "akka-persistence-query"            % akka,
    "com.typesafe.akka"             %% "akka-cluster-sharding"             % akka,
    "com.typesafe.akka"             %% "akka-discovery"                    % akka,
    "com.typesafe.akka"             %% "akka-http"                         % http,
    "com.typesafe.akka"             %% "akka-parsing"                      % http,
    "com.typesafe.akka"             %% "akka-http-spray-json"              % http,
    "com.lightbend.akka.management" %% "akka-management"                   % management,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % management,
    "com.lightbend.akka.management" %% "akka-management-cluster-http"      % management
  )
}

version := "1.3.3.9" // we hard-code the version here, it could be anything really
dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) =>
      Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }

dockerUsername := Some("local")
dockerExposedPorts := Seq(8080, 8558, 2552)
dockerBaseImage := "openjdk:8-jre-alpine"

dockerCommands ++= Seq(
  Cmd("USER", "root"),
  Cmd(
    "RUN",
    "/sbin/apk",
    "add",
    "--no-cache",
    "bash",
    "bind-tools",
    "busybox-extras",
    "curl",
    "strace"
  )
)
