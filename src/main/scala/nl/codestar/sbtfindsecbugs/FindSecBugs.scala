package nl.codestar.sbtfindsecbugs

import java.io.File

import sbt._
import Keys._

object FindSecBugs extends AutoPlugin {
  private val exitCodeOk: Int = 0
  private val exitCodeClassesMissing: Int = 2

  private val spotbugsVersion = "3.1.12"
  private val findsecbugsPluginVersion = "1.9.0"

  private val FindsecbugsConfig = sbt.config("findsecbugs")
    .describedAs("Classpath configuration for SpotBugs")
  private val FindSecBugsTag = Tags.Tag("findSecBugs")

  override def trigger = AllRequirements

  object autoImport {
    lazy val findSecBugsExcludeFile = settingKey[Option[File]]("The FindBugs exclude file for findsecbugs")
    lazy val findSecBugsFailOnMissingClass = settingKey[Boolean]("Consider 'missing class' flag as error")
    lazy val findSecBugsParallel = settingKey[Boolean]("Perform FindSecurityBugs check in parallel (or not)")
    lazy val findSecBugs = taskKey[Unit]("Perform FindSecurityBugs check")
  }

  import autoImport._

  override lazy val projectSettings = inConfig(FindsecbugsConfig)(Defaults.configSettings) ++ Seq(
    findSecBugsExcludeFile := None,
    findSecBugsFailOnMissingClass := true,
    findSecBugsParallel := true,
    concurrentRestrictions in Global ++= (if (findSecBugsParallel.value) Nil else Seq(Tags.exclusive(FindSecBugsTag))),
    ivyConfigurations += FindsecbugsConfig,
    libraryDependencies ++= Seq(
      "com.github.spotbugs" % "spotbugs" % spotbugsVersion % FindsecbugsConfig,
      "com.h3xstream.findsecbugs" % "findsecbugs-plugin" % findsecbugsPluginVersion % FindsecbugsConfig,
      "org.slf4j" % "slf4j-simple" % "1.8.0-beta4" % FindsecbugsConfig
    ),
    findSecBugs := (findSecBugsTask tag FindSecBugsTag).value
  )

  private def findSecBugsTask() = Def.task {
    def commandLineClasspath(classpathFiles: Seq[File]): String = PathFinder(classpathFiles.filter(_.exists)).absString
    lazy val log = Keys.streams.value.log
    lazy val output = crossTarget.value / "findsecbugs" / "report.html"
    lazy val classpath = commandLineClasspath((dependencyClasspath in FindsecbugsConfig).value.files)
    lazy val auxClasspath = commandLineClasspath((dependencyClasspath in Compile).value.files)
    lazy val ivyHome = ivyPaths(_.ivyHome).value.getOrElse(Path.userHome / ".ivy2")
    lazy val pluginList = s"${ivyHome.absolutePath}/cache/com.h3xstream.findsecbugs/findsecbugs-plugin/jars/findsecbugs-plugin-$findsecbugsPluginVersion.jar"
    lazy val classDirs = (products in Compile).value
    lazy val jHome = javaHome.value
    lazy val excludeFile = findSecBugsExcludeFile.value

    IO.createDirectory(output.getParentFile)
    IO.withTemporaryDirectory { tempdir =>
      val includeFile = createIncludesFile(tempdir)
      val filteredClassDirs = classDirs.filter(_.exists)
      if (filteredClassDirs.nonEmpty) {
        val filteredClassDirsStr = filteredClassDirs.map(cd => s"'$cd'").mkString(", ")
        log.info(s"Performing FindSecurityBugs check of $filteredClassDirsStr...")
        val findBugsLogger = new FindBugsLogger(log)
        val forkOptions = ForkOptions(
          javaHome = jHome,
          outputStrategy = Option(LoggedOutput(findBugsLogger)),
          bootJars = Vector.empty[java.io.File],
          workingDirectory = None,
          runJVMOptions = Vector.empty[String],
          connectInput = true,
          envVars = Map.empty[String, String])

        val opts = List("-Xmx1024m", "-cp", classpath, "edu.umd.cs.findbugs.LaunchAppropriateUI", "-textui",
          "-exitcode", "-html:plain.xsl", "-output", output.getAbsolutePath, "-nested:true",
          "-auxclasspath", auxClasspath, "-low", "-effort:max", "-pluginList", pluginList,
          "-noClassOk") ++
          List("-include", includeFile.getAbsolutePath) ++
          excludeFile.toList.flatMap(f => List("-exclude", f.getAbsolutePath)) ++
          filteredClassDirs.map(_.getAbsolutePath)
        val result = Fork.java(forkOptions, opts)
        result match {
          case `exitCodeOk` =>
            //noop
          case `exitCodeClassesMissing` if !findSecBugsFailOnMissingClass.value =>
            //noop
          case _ =>
            sys.error(s"Security issues found. Please review them in $output")
        }
      }
      else {
        val classDirsStr = classDirs.map(cd => s"'$cd'").mkString(", ")
        log.warn(s"Every class directory ($classDirsStr) does not exist or is empty, not running scan")
      }
    }
  }

  private def createIncludesFile(tempdir: sbt.File): sbt.File = {
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

  /**
    * FindBugs logs everyting to stderr, even when everything was succesful.
    * This logger makes that logging a little bit smarter.
    */
  class FindBugsLogger(underlying: Logger) extends Logger {
    override def log(level: Level.Value, message: => String): Unit = (level, message.toLowerCase) match {
      case (Level.Debug, _) =>
        underlying.log(Level.Debug, message)
      case (_, s) if s.contains("error") =>
        underlying.log(Level.Error, message)
      case (_, s) if s.contains("warning") =>
        underlying.log(Level.Warn, message)
      case _ =>
        underlying.log(Level.Info, message)
    }
    override def trace(t: => Throwable): Unit = underlying.trace(t)
    override def success(message: => String): Unit = underlying.success(message)
  }

}
