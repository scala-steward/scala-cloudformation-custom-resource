lazy val buildSettings = Seq(
  organization := "com.dwolla",
  name := "scala-cloudformation-custom-resource",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudformation-custom-resource")),
  description := "Abstract CloudFormation custom resource Lambda that can be easily extended with custom functionality.",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  version := "1.0.0",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  startYear := Option(2016),
  resolvers ++= Seq(
    Resolver.bintrayIvyRepo("dwolla", "maven")
  ),
  libraryDependencies ++= {
    val specs2Version = "3.8.6"
    val awsSdkVersion = "1.11.66"
    val json4sVersion = "3.5.0"

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
      "com.dwolla" %% "scala-aws-utils" % "1.0.0" exclude("com.amazonaws", "aws-java-sdk-cloudformation"),
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
      "com.amazonaws" % "aws-lambda-java-events" % "1.1.0" excludeAll(amazonJavaSdks: _*),
      "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.0" exclude("log4j", "log4j"),
      "com.amazonaws" % "aws-java-sdk-route53" % awsSdkVersion,
      "org.slf4j" % "log4j-over-slf4j" % "1.7.12",
      "org.json4s" %% "json4s-native" % json4sVersion,
      "org.json4s" %% "json4s-ext" % json4sVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.jsuereth" %% "scala-arm" % "2.0",
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-mock" % specs2Version % Test,
      "com.dwolla" %% "testutils" % "1.2.0" % Test
    )
  }
)

lazy val bintraySettings = Seq(
  bintrayVcsUrl := Some("https://github.com/Dwolla/scala-cloudformation-custom-resource"),
  publishMavenStyle := false,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false }
)

lazy val scalaAwsUtils = (project in file("."))
  .settings(buildSettings ++ bintraySettings: _*)
