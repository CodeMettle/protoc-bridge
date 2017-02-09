import ReleaseTransformations._

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions in ThisBuild := Seq("2.10.5", "2.11.7")

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 => List("-target:jvm-1.6")
    case _ => Nil
  }
}

javacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 => List("-target", "6", "-source", "6")
    case _ => Nil
  }
}

organization in ThisBuild := "com.trueaccord.scalapb"

name in ThisBuild := "protoc-bridge"

publishMavenStyle in ThisBuild := true

credentials in ThisBuild += {
  def file = "credentials-" + (if (isSnapshot.value) "snapshots" else "internal")

  Credentials(Path.userHome / ".m2" / file)
}

publishTo := {
  def path = "/repository/" + (if (isSnapshot.value) "snapshots" else "internal")

  Some("CodeMettle Maven" at s"http://maven.codemettle.com$path")
}

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  pushChanges,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true)
)

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "3.1.0",
  "commons-io" % "commons-io" % "2.5",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
