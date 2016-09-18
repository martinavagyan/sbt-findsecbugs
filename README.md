[![CircleCI](https://circleci.com/gh/code-star/sbt-findsecbugs.png)](https://circleci.com/gh/code-star/sbt-findsecbugs)

# sbt-findsecbugs
An SBT plugin for FindSecurityBugs

# Usage
Add to your `plugins.sbt`: `"nl.codestar" % "sbt-findsecbugs" % "(current version)"`

(You can find the current version [here](https://github.com/code-star/sbt-findsecbugs/releases).)

You can now run `sbt findSecBugs`.

# Configuration

sbt-findsecbugs has one setting:

|Setting|Default|Meaning|
|---|---|---|
|`findSecBugsParallel`|`true`|In a multimodule build, whether to run the security check for all submodules in parallel. If you run into memory issues, it might help to set this to `false`.|


# Tests
The plugin can be tested manually by running `sbt findSecBugs` in the test-project
The plugin has automated test which can be run by this command `sbt scripted`
