/** Project */
name := "scaladata"

version := "0.1-SNAPSHOT"

organization := "org.cakesolutions"

scalaVersion := "2.9.1"

/** Shell */
shellPrompt := { state => System.getProperty("user.name") + "> " }

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

/** Dependencies */
resolvers ++= Seq("sbt-idea-repo" at "http://mpeltonen.github.com/maven/",
                  "snapshots-repo" at "http://scala-tools.org/repo-snapshots", 
                  "Local Maven Repository" at "file://$M2_REPO")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "0.11.0")


libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.3"

libraryDependencies += "org.neo4j" %% "neo4j" % "1.4.2"

/** Compilation */
javacOptions ++= Seq("-Xmx1812m", "-Xms512m", "-Xss4m")

javaOptions += "-Xmx2G"

scalacOptions += "-deprecation"

maxErrors := 20 

pollInterval := 1000

testOptions := Seq(Tests.Filter(s =>
  Seq("Spec", "Suite", "Unit", "all").exists(s.endsWith(_)) &&
    ! s.endsWith("FeaturesSpec") ||
    s.contains("UserGuide") || 
    s.matches("org.specs2.guide.*")))

// Packaging

/** Publishing */
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

//publishTo <<= (version) { version: String =>
//  val nexus = "http://nexus-direct.scala-tools.org/content/repositories/"
//  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus+"snapshots/") 
//  else                                   Some("releases" at nexus+"releases/")
//}
