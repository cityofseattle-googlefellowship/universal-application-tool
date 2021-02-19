lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)
  .settings(
    name := """universal-application-tool""",
    version := "0.0.1",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      // JSON libraries
      "com.jayway.jsonpath" % "json-path" % "2.5.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.10.3",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.10.3",

      // Templating
      "com.j2html" % "j2html" % "1.4.0",

      // Amazon AWS SDK
      "software.amazon.awssdk" % "aws-sdk-java" % "2.15.81",
      // Database and database testing libraries
      "org.postgresql" % "postgresql" % "42.2.18",
      "org.testcontainers" % "postgresql" % "1.15.1" % Test,
      "org.testcontainers" % "testcontainers" % "1.15.1" % Test,
      "org.testcontainers" % "junit-jupiter" % "1.15.1" % Test,
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.4.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-api" % "5.4.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-params" % "5.4.2" % Test,
      "com.h2database" % "h2" % "1.4.199" % Test,

      // Testing libraries for dealing with CompletionStage...
      "org.assertj" % "assertj-core" % "3.14.0" % Test,
      "org.awaitility" % "awaitility" % "4.0.1" % Test,
      "org.mockito" % "mockito-core" % "3.1.0" % Test,

      // To provide an implementation of JAXB-API, which is required by Ebean.
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "javax.activation" % "activation" % "1.1.1",
      "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.2",

      // Security libraries
      // pac4j core (https://github.com/pac4j/play-pac4j)
      "org.pac4j" %% "play-pac4j" % "11.0.0-PLAY2.8-RC1",
      "org.pac4j" % "pac4j-core" % "5.0.0-RC1",
      // basic http authentication (for now)
      "org.pac4j" % "pac4j-http" % "5.0.0-RC1",
      "org.apache.shiro" % "shiro-crypto-cipher" % "1.7.1",

      // Autovalue
      "com.google.auto.value" % "auto-value-annotations" % "1.7.4",
      "com.google.auto.value" % "auto-value" % "1.7.4",
      "com.google.auto.value" % "auto-value-parent" % "1.7.4",
    ),
    javacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-parameters",
      "-Xlint:unchecked",
      "-Xlint:deprecation",
      "-Werror"
    ),
    // Make verbose tests
    testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))
  )
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
resolvers += Resolver.bintrayRepo("webjars","maven")
libraryDependencies ++= Seq(
    "org.webjars.npm" % "react" % "15.4.0",
    "org.webjars.npm" % "types__react" % "15.0.34"
)
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.10.5",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.10.5",
)
resolveFromWebjarsNodeModulesDir := true
