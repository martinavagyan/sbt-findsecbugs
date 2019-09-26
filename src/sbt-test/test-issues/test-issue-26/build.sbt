val checkDependencyLeaks = taskKey[Unit]("Check that plugin's dependencies don't leak into the project itself")

// Same as in the plugin itself - we don't want to expose this yet.
val FindsecbugsConfig = sbt.config("findsecbugs")

checkDependencyLeaks := {
  val compileCP = (Compile / dependencyClasspath).value.files.toSet
  val spotbugsCP = (FindsecbugsConfig / dependencyClasspath).value.files.toSet

  // Make sure we have the right configuration.
  assert(spotbugsCP.size > 2)
  // Make sure none of these libraries have leaked into the project itself.
  assert(spotbugsCP.intersect(compileCP).isEmpty)
}
