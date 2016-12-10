package com.dwolla.lambda.cloudformation

import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class CloudFormationCustomResourceResponseWriter(implicit ec: ExecutionContext) {
  protected lazy val logger: Logger = LoggerFactory.getLogger("LambdaLogger")
  protected implicit val formats: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  def httpClient: CloseableHttpClient = HttpClients.createDefault()

  def logAndWriteToS3(presignedUri: String, cloudFormationCustomResourceResponse: CloudFormationCustomResourceResponse): Future[Unit] = Future {
    val req = new HttpPut(presignedUri)
    val jsonEntity = new StringEntity(write(cloudFormationCustomResourceResponse))
    jsonEntity.setContentType("")

    req.setEntity(jsonEntity)
    req.addHeader("Content-Type", "")

    logger.info(Source.fromInputStream(req.getEntity.getContent).mkString)

    try {
      val res = httpClient.execute(req)
      res.close()
    } finally {
      httpClient.close()
    }
  }
}
