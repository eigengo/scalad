import sbtrelease._

/** Project */
name := "Scalad"

organization := "com.sefaira"

scalaVersion := "2.10.1"

/** Dependencies */
resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq("Sefaira Repo" at "https://sefaira.artifactoryonline.com/sefaira/main-virtual-repo")

publishTo := Some("releases" at "https://sefaira.artifactoryonline.com/sefaira/libs-local-ivy")

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>http://www.eigengo.org/scalad.html</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:janm399/scalad.git</url>
    <connection>scm:git:git@github.com:janm399/scalad.git</connection>
  </scm>
  <developers>
    <developer>
      <id>janmachacek</id>
      <name>Jan Machacek</name>
      <url>http://www.eigengo.org</url>
    </developer>
    <developer>
      <id>anirvanchakraborty</id>
      <name>Anirvan Chakraborty</name>
      <url>http://www.eigengo.org</url>
    </developer>
  </developers>
)

libraryDependencies <<= scalaVersion { scala_version =>
    Seq(
        "com.github.fommil"    % "java-logging"        % "1.0",
        "com.typesafe.akka"    %% "akka-actor"         % "2.1.2",
        "com.typesafe.akka"    %% "akka-contrib"       % "2.1.2" intransitive(), // JUL only
        "org.mongodb"          % "mongo-java-driver"   % "2.10.1",
        "com.typesafe"         % "config"              % "1.0.0",
        "io.spray"             %% "spray-json"         % "1.2.3",
        "org.joda"             %  "joda-convert"       % "1.2",
        "joda-time"            % "joda-time"           % "2.3",
        "org.specs2"           %% "specs2"             % "1.13"   % "test",
        "org.scalacheck"       %% "scalacheck"         % "1.10.0" % "test"
    )
}

/** Compilation */
javacOptions ++= Seq("-Xmx1812m", "-Xms512m", "-Xss6m")

javaOptions += "-Xmx2G"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

maxErrors := 20 

pollInterval := 1000

logBuffered := false

cancelable := true

testOptions := Seq(Tests.Filter(s =>
  Seq("Spec", "Suite", "Test", "Unit", "all").exists(s.endsWith(_)) &&
    !s.endsWith("FeaturesSpec") ||
    s.contains("UserGuide") || 
    s.contains("index") ||
    s.matches("org.specs2.guide.*")))

/** Console */
initialCommands in console := "import org.eigengo.scalad._"

