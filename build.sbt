import org.scalajs.linker.interface.ESVersion
import com.typesafe.tools.mima.core._

val projectName = "parsley"
val Scala213 = "2.13.10"
val Scala212 = "2.12.17"
val Scala3 = "3.2.1"
val Java8 = JavaSpec.temurin("8")
val JavaLTS = JavaSpec.temurin("11")
val JavaLatest = JavaSpec.temurin("17")

val mainBranch = "master"

Global / onChangedBuildSource := ReloadOnSourceChanges

val isInPublish = Option(System.getenv("GITHUB_JOB")).contains("publish")
val releaseFlags = Seq("-Xdisable-assertions", "-opt:l:method,inline", "-opt-inline-from", "parsley.**", "-opt-warnings:at-inline-failed")

inThisBuild(List(
  tlBaseVersion := "4.2",
  organization := "com.github.j-mie6",
  startYear := Some(2018),
  homepage := Some(url("https://github.com/j-mie6/parsley")),
  licenses := List("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")),
  developers := List(
    tlGitHubDev("j-mie6", "Jamie Willis")
  ),
  versionScheme := Some("early-semver"),
  crossScalaVersions := Seq(Scala213, Scala212, Scala3),
  scalaVersion := Scala213,
  mimaBinaryIssueFilters ++= Seq(
    ProblemFilters.exclude[Problem]("parsley.internal.*"),
    ProblemFilters.exclude[Problem]("parsley.X*"),
    // Until 5.0 (these are all misreported package private members)
    ProblemFilters.exclude[DirectMissingMethodProblem]("parsley.token.numeric.Combined.this"),
    ProblemFilters.exclude[MissingClassProblem]("parsley.token.text.RawCharacter$"),
    ProblemFilters.exclude[DirectMissingMethodProblem]("parsley.token.symbol.Symbol.this"),
    ProblemFilters.exclude[ReversedMissingMethodProblem]("parsley.token.numeric.Integer.bounded"),
    ProblemFilters.exclude[MissingClassProblem]("parsley.token.numeric.Generic$"),
    ProblemFilters.exclude[MissingClassProblem]("parsley.token.predicate$_CharSet$"),
    ProblemFilters.exclude[MissingFieldProblem]("parsley.token.predicate._CharSet"),
    ProblemFilters.exclude[MissingClassProblem]("parsley.token.errors.ErrorConfig$"),
    ProblemFilters.exclude[DirectMissingMethodProblem]("parsley.errors.combinator#ErrorMethods.unexpected"),
    ProblemFilters.exclude[MissingClassProblem]("parsley.token.errors.FilterOps"),
    ProblemFilters.exclude[MissingClassProblem]("parsley.token.errors.FilterOps$"),
    ProblemFilters.exclude[ReversedMissingMethodProblem]("parsley.token.predicate#CharPredicate.asInternalPredicate")
  ),
  tlVersionIntroduced := Map(
    "2.13" -> "1.5.0",
    "2.12" -> "1.5.0",
    "3"    -> "3.1.2",
  ),
  // CI Configuration
  tlCiReleaseBranches := Seq(mainBranch),
  tlSonatypeUseLegacyHost := false,
  githubWorkflowJavaVersions := Seq(Java8, JavaLTS, JavaLatest),
  // We need this because our release uses different flags
  githubWorkflowArtifactUpload := false,
  githubWorkflowAddedJobs += testCoverageJob(githubWorkflowGeneratedCacheSteps.value.toList),
))

lazy val root = tlCrossRootProject.aggregate(parsley)

lazy val parsley = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("parsley"))
  .settings(
    name := projectName,

    resolvers ++= Opts.resolver.sonatypeOssReleases, // Will speed up MiMA during fast back-to-back releases
    libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % "3.2.15" % Test,
        "org.scalatestplus" %%% "scalacheck-1-17" % "3.2.15.0" % Test,
    ),

    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oI"),

    scalacOptions ++= (if (isInPublish) releaseFlags else Seq.empty),

    Compile / doc / scalacOptions ++= Seq("-groups", "-doc-root-content", s"${baseDirectory.value.getParentFile.getPath}/rootdoc.md"),
    Compile / doc / scalacOptions ++= {
        if (scalaBinaryVersion.value == "3") Seq("-comment-syntax:wiki") else Seq.empty
    },
  )
  .jsSettings(
    Test / scalaJSLinkerConfig := scalaJSLinkerConfig.value.withESFeatures(_.withESVersion(ESVersion.ES2018)),
    Compile / bloopGenerate := None,
    // JS lacks the IO module, so has its own rootdoc
    Compile / doc / scalacOptions ++= Seq("-groups", "-doc-root-content", s"${baseDirectory.value.getPath}/rootdoc.md"),
    Test / bloopGenerate := None,
  )
  .nativeSettings(
    Compile / bloopGenerate := None,
    Test / bloopGenerate := None,
  )

def testCoverageJob(cacheSteps: List[WorkflowStep]) = WorkflowJob(
    id = "coverage",
    name = "Run Test Coverage and Upload",
    scalas = List(Scala213),
    cond = Some(s"github.ref == 'refs/heads/$mainBranch' || (github.event_name == 'pull_request' && github.base_ref == '$mainBranch')"),
    steps =
        WorkflowStep.Checkout ::
        WorkflowStep.SetupJava(List(JavaLTS)) :::
        cacheSteps ::: List(
            WorkflowStep.Sbt(name = Some("Generate coverage report"), commands = List("coverage", "parsley / test", "coverageReport")),
            WorkflowStep.Use(
                name = Some("Upload coverage to Code Climate"),
                ref = UseRef.Public(owner = "paambaati", repo = "codeclimate-action", ref = "v3.2.0"),
                env = Map("CC_TEST_REPORTER_ID" -> "${{secrets.CC_TEST_REPORTER_ID}}"),
                params = Map("coverageLocations" -> "${{github.workspace}}/parsley/jvm/target/scala-2.13/coverage-report/cobertura.xml:cobertura"),
            )
        )
)
