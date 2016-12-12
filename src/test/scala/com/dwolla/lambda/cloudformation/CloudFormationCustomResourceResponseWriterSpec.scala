package com.dwolla.lambda.cloudformation

import java.io.InputStreamReader
import java.net.URI

import com.dwolla.testutils.exceptions.NoStackTraceException
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPut, HttpUriRequest}
import org.apache.http.impl.client.CloseableHttpClient
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import org.slf4j.Logger
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CloudFormationCustomResourceResponseWriterSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    protected implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
    val mockHttpClient = mock[CloseableHttpClient]
    val mockLogger = mock[Logger]

    val responseWriter = new CloudFormationCustomResourceResponseWriter {
      override def httpClient: CloseableHttpClient = mockHttpClient

      override protected lazy val logger: Logger = mockLogger
    }

    val sampleCloudFormationCustomResourceResponse = CloudFormationCustomResourceResponse(
      Status = "status",
      Reason = Option("reason"),
      PhysicalResourceId = Option("physical-resource-id"),
      StackId = "stack-id",
      RequestId = "request-id",
      LogicalResourceId = "logical-resource-id"
    )
  }

  "CloudFormationCustomResourceResponseWriter" should {

    "PUT the JSONified response to the presigned URL" in new Setup {
      val httpRequestCaptor = capture[HttpPut]
      private val mockResponse = mock[CloseableHttpResponse]
      mockHttpClient.execute(httpRequestCaptor) returns mockResponse

      val output = responseWriter.logAndWriteToS3("https://dwolla.amazonaws.com", sampleCloudFormationCustomResourceResponse)

      output must be_==(()).await

      httpRequestCaptor.value.getMethod must_== "PUT"
      httpRequestCaptor.value.getURI must_== new URI("https://dwolla.amazonaws.com")
      httpRequestCaptor.value.getFirstHeader("Content-Type").getValue must_== ""
      private val httpEntity = httpRequestCaptor.value.getEntity
      httpEntity.getContentType.getValue must_== ""
      httpEntity.getContentLength must be_>(0L)
      httpEntity.isChunked must beFalse

      read[CloudFormationCustomResourceResponse](new InputStreamReader(httpEntity.getContent)) must_== sampleCloudFormationCustomResourceResponse

      there was one(mockLogger).info(write(sampleCloudFormationCustomResourceResponse))
      there was one(mockResponse).close()
      there was one(mockHttpClient).close()
    }

    "close the HTTP client even if the PUT throws an error" in new Setup {
      mockHttpClient.execute(any[HttpUriRequest]) throws NoStackTraceException

      val output = responseWriter.logAndWriteToS3("https://dwolla.amazonaws.com", sampleCloudFormationCustomResourceResponse)

      output must throwA(NoStackTraceException).await
      there was one(mockHttpClient).close()
    }

  }

}
