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
    val specs2Version = "4.1.1" // have to pin to 4.1.x due to https://github.com/typelevel/cats/issues/2449
    val catsVersion = "1.5.0"
    val catsEffectVersion = "0.10.1"
    val circeVersion = "0.9.3"

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
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
      "com.amazonaws" % "aws-lambda-java-log4j2" % "1.0.0",
      "org.log4s" %% "log4s" % "1.6.1",
      "org.apache.logging.log4j" % "log4j-api" % "2.11.1",
      "org.apache.logging.log4j" % "log4j-core" % "2.11.1",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.1",
      "org.apache.httpcomponents" % "httpclient" % "4.5.2",
      "com.amazonaws" % "aws-lambda-java-events" % "1.1.0" % Test excludeAll(amazonJavaSdks: _*),
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-mock" % specs2Version % Test,
      "com.dwolla" %% "testutils" % "1.10.0" % Test,
      "io.circe" %% "circe-literal" % circeVersion % Test,
      "org.typelevel" %% "cats-laws" % catsVersion % Test,
      "org.specs2" %% "specs2-scalacheck" % specs2Version % Test,
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser",
    ).map(_ % circeVersion)
  },
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
