lazy val buildSettings = Seq(
  organization := "com.dwolla",
  name := "scala-cloudformation-custom-resource",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudformation-custom-resource")),
  description := "Abstract CloudFormation custom resource Lambda that can be easily extended with custom functionality.",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Option(2016),
  resolvers ++= Seq(
    Resolver.bintrayRepo("dwolla", "maven")
  ),
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  releaseCrossBuild := true,
  libraryDependencies ++= {
    val specs2Version = "4.3.0"
    val catsVersion = "1.1.0"
    val catsEffectVersion = "0.10.1"
    val circeVersion = "0.10.0-M1"

    val amazonJavaSdks = List(    // exclude the SDKs we don't need, since they're pulled in transitively, to keep the size of the jar down
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-cloudformation"),
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-cognitoidentity"),
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-dynamodb"),
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-kinesis"),
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-kms"),
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-s3"),
      ExclusionRule(organization = "com.amazonaws", name = "aws-java-sdk-sqs")
    )

    Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.dwolla" %% "scala-aws-utils" % "1.6.1",
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
      "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.0" exclude("log4j", "log4j"),
      "org.apache.httpcomponents" % "httpclient" % "4.5.2",
      "org.slf4j" % "log4j-over-slf4j" % "1.7.20",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.jsuereth" %% "scala-arm" % "2.0",
      "com.amazonaws" % "aws-lambda-java-events" % "1.1.0" % Test excludeAll(amazonJavaSdks: _*),
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-mock" % specs2Version % Test,
      "com.dwolla" %% "testutils" % "1.10.0" % Test,
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  },
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
//    "-Xlog-implicits",                 // normally left disabled because it's super noisy
  ) ++ (scalaBinaryVersion.value match {
    case "2.12" ⇒ Seq(
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
      "-Ywarn-unused:explicits",           // Warn if an explicit parameter is unused.
      "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",              // Warn if a local definition is unused.
      "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",            // Warn if a private member is unused.
    )
    case _ ⇒ Seq.empty
  }),
  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  scalacOptions in Compile in Test -= "-Xfatal-warnings",
)

lazy val bintraySettings = Seq(
  bintrayVcsUrl := homepage.value.map(_.toString),
  publishMavenStyle := true,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false }
)

lazy val cloudformationCustomResource = (project in file("."))
  .settings(buildSettings ++ bintraySettings: _*)
