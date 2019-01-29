package com.dwolla.lambda.cloudformation

import java.io._

import cats.effect._
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.util.StringInputStream
import com.dwolla.lambda.cloudformation.SampleMessages._
import com.dwolla.testutils.exceptions.NoStackTraceException
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.mockito._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.apache.logging.log4j._

import scala.concurrent._

class CloudFormationCustomResourceHandlerSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  implicit def listStringToJson(l: List[String]): Json = Json.arr(l.map(Json.fromString): _*)

  val spaces2OmitNulls = Printer.indented("  ").copy(dropNullValues = true, colonLeft = "", colonRight = "")

  "IO-based Handler" should {
    trait IOSetup extends AbstractCustomResourceHandler[IO] with Scope {
      val context = mock[Context]
      val mockLogger = mock[Logger]
      val outputStream = mock[OutputStream]
      val mockResponseWriter = mock[CloudFormationCustomResourceResponseWriter[IO]]

      override protected lazy val logger: Logger = mockLogger

      override protected def responseWriter = mockResponseWriter
    }

    "deserialize input, pass req to handler, and convert output to response format" in new IOSetup {
      val promisedRequest = Promise[CloudFormationCustomResourceRequest]

      override def handleRequest(input: CloudFormationCustomResourceRequest) = IO {
        promisedRequest.success(input)
        HandlerResponse(physicalId = "physical-id")
      }

      private val expectedResponse = CloudFormationCustomResourceResponse(
        Status = "SUCCESS",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        LogicalResourceId = "MyTestResource",
        RequestId = "unique id for this create request",
        PhysicalResourceId = Option("physical-id"),
        Reason = None
      )

      mockResponseWriter.logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse) returns IO.unit

      this.handleRequest(new StringInputStream(CloudFormationCustomResourceInputJson), outputStream, context)

      promisedRequest.future must be_==(CloudFormationCustomResourceRequest(
        RequestType = "Create",
        ResponseURL = "http://pre-signed-S3-url-for-response",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        RequestId = "unique id for this create request",
        ResourceType = "Custom::TestResource",
        LogicalResourceId = "MyTestResource",
        PhysicalResourceId = None,
        ResourceProperties = Option(Map("StackName" → Json.fromString("stack-name"), "List" → Json.arr(List("1", "2", "3").map(Json.fromString): _*))),
        OldResourceProperties = None
      )).await
      there was one(mockResponseWriter).logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse)
    }

    "log json if a parse error occurs" in new IOSetup {
      this.handleRequest(new StringInputStream(invalidJson), outputStream, context) must throwA[ParsingFailure]

      there was one(mockLogger).error(ArgumentMatchers.eq(
        s"""Could not parse the following input:
           |$invalidJson""".stripMargin), any[ParsingFailure])

      //noinspection NotImplementedCode
      override def handleRequest(req: CloudFormationCustomResourceRequest) = ???
    }

    "return a failure if the handler throws an exception" in new IOSetup {
      override def handleRequest(req: CloudFormationCustomResourceRequest) = IO.raiseError(NoStackTraceException)

      private val expectedResponse = CloudFormationCustomResourceResponse(
        Status = "FAILED",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        LogicalResourceId = "MyTestResource",
        RequestId = "unique id for this create request",
        PhysicalResourceId = None,
        Reason = Option("exception intentionally thrown by test"),
        Data = Map("StackTrace" → List("com.dwolla.testutils.exceptions.NoStackTraceException$: exception intentionally thrown by test"))
      )

      mockResponseWriter.logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse) returns IO.unit

      this.handleRequest(new StringInputStream(CloudFormationCustomResourceInputJson), outputStream, context)

      there was one(mockResponseWriter).logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse)
    }
  }

  "Response" should {
    "be serializable" >> {
      val exception = new WritableStackTraceRuntimeException
      exception.setStackTrace(Array(new StackTraceElement("class", "method", "filename", 42)))
      exception.addSuppressed(new WritableStackTraceRuntimeException("suppressed exception intentionally thrown by test"))

      val stackTrace = {
        val out = new StringWriter()
        exception.printStackTrace(new PrintWriter(out))
        out.toString.lines.toList
      }

      val expectedResponse = CloudFormationCustomResourceResponse(
        Status = "FAILED",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        LogicalResourceId = "MyTestResource",
        RequestId = "unique id for this create request",
        PhysicalResourceId = None,
        Reason = Option("exception intentionally thrown by test"),
        Data = Map(
          "StackTrace" → stackTrace
        )
      )

      expectedResponse.asJson.pretty(spaces2OmitNulls) must_==
        """{
          |  "Status":"FAILED",
          |  "Reason":"exception intentionally thrown by test",
          |  "StackId":"arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
          |  "RequestId":"unique id for this create request",
          |  "LogicalResourceId":"MyTestResource",
          |  "Data":{
          |    "StackTrace":[
          |      "com.dwolla.lambda.cloudformation.WritableStackTraceRuntimeException: exception intentionally thrown by test",
          |      "\tat class.method(filename:42)",
          |      "\tSuppressed: com.dwolla.lambda.cloudformation.WritableStackTraceRuntimeException: suppressed exception intentionally thrown by test"
          |    ]
          |  }
          |}""".stripMargin
    }
  }
}

object SampleMessages {
  val CloudFormationCustomResourceInputJson =
    """{
      |  "StackId": "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
      |  "ResponseURL": "http://pre-signed-S3-url-for-response",
      |  "ResourceProperties": {
      |    "StackName": "stack-name",
      |    "List": [
      |      "1",
      |      "2",
      |      "3"
      |    ]
      |  },
      |  "RequestType": "Create",
      |  "ResourceType": "Custom::TestResource",
      |  "RequestId": "unique id for this create request",
      |  "LogicalResourceId": "MyTestResource"
      |}
      | """.stripMargin

  val invalidJson = "}"
}

class WritableStackTraceRuntimeException(message: String = "exception intentionally thrown by test") extends RuntimeException(message, null, true, true) {
  override def fillInStackTrace(): Throwable = this
}
