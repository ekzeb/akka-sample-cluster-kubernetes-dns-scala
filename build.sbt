import com.typesafe.sbt.packager.docker._

scalaVersion := "2.13.1"
scalacOptions ++= Seq("-deprecation", "-feature", "-language:pos")

enablePlugins(JavaServerAppPackaging)

libraryDependencies ++= {
  val managementV = "1.0.3"
  val akkaV       = "2.5.25"
  Seq(
    "com.typesafe.akka"             %% "akka-actor-typed"                  % akkaV,
    "com.typesafe.akka"             %% "akka-cluster-typed"                % akkaV,
    "com.lightbend.akka.management" %% "akka-management"                   % managementV,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % managementV,
    "com.lightbend.akka.management" %% "akka-management-cluster-http"      % managementV
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
