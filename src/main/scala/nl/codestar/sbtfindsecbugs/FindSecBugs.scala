package nl.codestar.sbtfindsecbugs

import java.io.File

import sbt._
import Keys._

object FindSecBugs extends AutoPlugin {
  val findsecbugsConfig = sbt.config("findsecbugs")

  override def trigger = AllRequirements

  object autoImport {
    lazy val findSecBugs = taskKey[Unit]("Perform FindSecurityBugs check")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    ivyConfigurations += findsecbugsConfig,
    libraryDependencies ++= Seq(
      "com.google.code.findbugs" % "findbugs" % "3.0.1",
      "com.google.code.findbugs" % "jsr305" % "3.0.1",
      "com.h3xstream.findsecbugs" % "findsecbugs-plugin" % "1.4.5"),
    findSecBugs := {
      def commandLineClasspath(classpathFiles: Seq[File]): String = PathFinder(classpathFiles).absString
      lazy val output = crossTarget.value / "findsecbugs" / "report.html"
      lazy val findbugsClasspath = Classpaths managedJars (findsecbugsConfig, classpathTypes.value, update.value)
      lazy val classpath = commandLineClasspath((dependencyClasspath in Compile).value.files)
      lazy val auxClasspath = (baseDirectory.value / "src").absString
      lazy val pluginList = s"${Path.userHome.absolutePath}/.ivy2/cache/com.h3xstream.findsecbugs/findsecbugs-plugin/jars/findsecbugs-plugin-1.4.5.jar"

      IO.createDirectory(output.getParentFile)
      IO.withTemporaryDirectory { tempdir =>
        val includeFile: sbt.File = createIncludesFile(tempdir)

        Fork.java(
          ForkOptions(javaHome = javaHome.value, outputStrategy = Some(new LoggedOutput(streams.value.log))),
          List("-Xmx1024m", "-cp", classpath, "edu.umd.cs.findbugs.LaunchAppropriateUI",
            "-textui", "-html:plain.xsl", "-output", output.getAbsolutePath, "-nested:true", "-auxclasspath", auxClasspath,
            "-low", "-effort:max", "-pluginList", pluginList, "-include", includeFile.getAbsolutePath,
            (classDirectory in Compile).value.getAbsolutePath))
      }

      def createIncludesFile(tempdir: sbt.File): sbt.File = {
        val includeFile = tempdir / "include.xml"
        val includeXml =
          """
            |<FindBugsFilter>
            |    <Match>
            |        <Bug category="SECURITY"/>
            |    </Match>
            |</FindBugsFilter>
          """.stripMargin
        IO.write(includeFile, includeXml)
        includeFile
      }
    })
}
