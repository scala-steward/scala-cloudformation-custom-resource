package com.dwolla.lambda.cloudformation

import java.net.URI

import cats.effect._
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import com.dwolla.testutils.exceptions.NoStackTraceException
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPut, HttpUriRequest}
import org.apache.http.impl.client.CloseableHttpClient
import org.slf4j.Logger
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.io.Source

class CloudFormationCustomResourceResponseWriterSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    val mockHttpClient = mock[CloseableHttpClient]
    val mockLogger = mock[Logger]

    val responseWriter = new CloudFormationCustomResourceResponseWriter[IO] {
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

      val output = responseWriter.logAndWriteToS3("https://dwolla.amazonaws.com", sampleCloudFormationCustomResourceResponse).unsafeToFuture()

      output must be_==(()).await

      httpRequestCaptor.value.getMethod must_== "PUT"
      httpRequestCaptor.value.getURI must_== new URI("https://dwolla.amazonaws.com")
      httpRequestCaptor.value.getFirstHeader("Content-Type").getValue must_== ""
      private val httpEntity = httpRequestCaptor.value.getEntity
      httpEntity.getContentType.getValue must_== ""
      httpEntity.getContentLength must be_>(0L)
      httpEntity.isChunked must beFalse

      val response = parse(Source.fromInputStream(httpEntity.getContent).mkString).flatMap(_.as[CloudFormationCustomResourceResponse]).right.get

      response must_== sampleCloudFormationCustomResourceResponse

      there was one(mockLogger).info(sampleCloudFormationCustomResourceResponse.asJson.noSpaces)
      there was one(mockResponse).close()
      there was one(mockHttpClient).close()
    }

    "response reason field is omitted if it is None" in new Setup {
      val httpRequestCaptor = capture[HttpPut]
      private val mockResponse = mock[CloseableHttpResponse]
      mockHttpClient.execute(httpRequestCaptor) returns mockResponse

      val output = responseWriter.logAndWriteToS3("https://dwolla.amazonaws.com", sampleCloudFormationCustomResourceResponse.copy(Reason = None)).unsafeToFuture()

      output must be_==(()).await

      private val httpEntity = httpRequestCaptor.value.getEntity

      val response = Source.fromInputStream(httpEntity.getContent).mkString

      response must_== """{"Status":"status","PhysicalResourceId":"physical-resource-id","StackId":"stack-id","RequestId":"request-id","LogicalResourceId":"logical-resource-id","Data":{}}"""
    }

    "close the HTTP client even if the PUT throws an error" in new Setup {
      mockHttpClient.execute(any[HttpUriRequest]) throws NoStackTraceException

      val output = responseWriter.logAndWriteToS3("https://dwolla.amazonaws.com", sampleCloudFormationCustomResourceResponse).unsafeToFuture()

      output must throwA(NoStackTraceException).await
      there was one(mockHttpClient).close()
    }

  }

}
