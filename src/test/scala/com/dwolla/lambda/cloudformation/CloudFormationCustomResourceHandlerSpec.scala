package com.dwolla.lambda.cloudformation

import java.io.OutputStream

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.util.StringInputStream
import com.dwolla.lambda.cloudformation.SampleMessages._
import com.dwolla.testutils.exceptions.NoStackTraceException
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.ParserUtil.ParseException
import org.mockito.Matchers
import org.slf4j.Logger
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.{ExecutionContext, Future, Promise}

class CloudFormationCustomResourceHandlerSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    def mockHandlerRequest(input: CloudFormationCustomResourceRequest): Future[HandlerResponse]

    val context = mock[Context]
    val mockLogger = mock[Logger]
    val outputStream = mock[OutputStream]
    val mockResponseWriter = mock[CloudFormationCustomResourceResponseWriter]
    val promisedShutdownOfActualHandler = Promise[Unit]

    val handler = new AbstractCustomResourceHandler() {
      override protected lazy val logger: Logger = mockLogger

      override def createParsedRequestHandler(): ParsedCloudFormationCustomResourceRequestHandler = new ParsedCloudFormationCustomResourceRequestHandler {
        override def shutdown(): Unit = promisedShutdownOfActualHandler.success(())

        override def handleRequest(input: CloudFormationCustomResourceRequest): Future[HandlerResponse] = mockHandlerRequest(input)
      }

      override protected def responseWriter = mockResponseWriter

      override implicit val executionContext: ExecutionContext = ee.executionContext
    }
  }

  "Handler" should {
    "deserialize input, pass req to handler, and convert output to response format" in new Setup {
      val promisedRequest = Promise[CloudFormationCustomResourceRequest]
      override def mockHandlerRequest(input: CloudFormationCustomResourceRequest) = Future {
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

      mockResponseWriter.logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse) returns Future.successful(())

      handler.handleRequest(new StringInputStream(CloudFormationCustomResourceInputJson), outputStream, context)

      promisedRequest.future must be_==(CloudFormationCustomResourceRequest(
        RequestType = "Create",
        ResponseURL = "http://pre-signed-S3-url-for-response",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        RequestId = "unique id for this create request",
        ResourceType = "Custom::TestResource",
        LogicalResourceId = "MyTestResource",
        PhysicalResourceId = None,
        ResourceProperties = Option(Map("StackName" → JString("stack-name"), "List" → JArray(List("1", "2", "3").map(JString)))),
        OldResourceProperties = None
      )).await
      there was one(mockResponseWriter).logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse)

      promisedShutdownOfActualHandler.future must be_==(()).await
    }

    "log json if a parse error occurs" in new Setup {
      handler.handleRequest(new StringInputStream(invalidJson), outputStream, context) must throwA[ParseException]

      there was one(mockLogger).error(Matchers.eq(invalidJson), any[ParseException])

      //noinspection NotImplementedCode
      override def mockHandlerRequest(input: CloudFormationCustomResourceRequest) = ???
    }

    "return a failure if the handler throws an exception" in new Setup {
      override def mockHandlerRequest(input: CloudFormationCustomResourceRequest) = throw NoStackTraceException

      private val expectedResponse = CloudFormationCustomResourceResponse(
        Status = "FAILED",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        LogicalResourceId = "MyTestResource",
        RequestId = "unique id for this create request",
        PhysicalResourceId = None,
        Reason = Option("exception intentionally thrown by test")
      )

      mockResponseWriter.logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse) returns Future.successful(())

      handler.handleRequest(new StringInputStream(CloudFormationCustomResourceInputJson), outputStream, context)

      there was one(mockResponseWriter).logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse)

      promisedShutdownOfActualHandler.future must be_==(()).await
    }

    "return a failure if the handler returns a failed Future" in new Setup {
      override def mockHandlerRequest(input: CloudFormationCustomResourceRequest) = Future.failed(NoStackTraceException)

      private val expectedResponse = CloudFormationCustomResourceResponse(
        Status = "FAILED",
        StackId = "arn:aws:cloudformation:us-west-2:EXAMPLE/stack-name/guid",
        LogicalResourceId = "MyTestResource",
        RequestId = "unique id for this create request",
        PhysicalResourceId = None,
        Reason = Option("exception intentionally thrown by test")
      )

      mockResponseWriter.logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse) returns Future.successful(())

      handler.handleRequest(new StringInputStream(CloudFormationCustomResourceInputJson), outputStream, context)

      there was one(mockResponseWriter).logAndWriteToS3("http://pre-signed-S3-url-for-response", expectedResponse)
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
