package com.dwolla.lambda.cloudformation

import cats.effect._
import com.dwolla.lambda.cloudformation.CloudFormationCustomResourceResponseWriter._
import io.circe._
import io.circe.syntax._
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client._
import org.apache.logging.log4j._

import scala.io.Source
import scala.language.higherKinds

class CloudFormationCustomResourceResponseWriter[F[_]: Async] {
  protected lazy val logger: Logger = LogManager.getLogger("LambdaLogger")

  protected def httpClient: CloseableHttpClient = HttpClients.createDefault()

  def logAndWriteToS3(presignedUri: String, cloudFormationCustomResourceResponse: CloudFormationCustomResourceResponse): F[Unit] =
    Async[F].delay {
      val req = new HttpPut(presignedUri)
      val jsonEntity = new StringEntity(cloudFormationCustomResourceResponse.asJson.pretty(compactPrinter))
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

object CloudFormationCustomResourceResponseWriter {
  private[CloudFormationCustomResourceResponseWriter] val compactPrinter = Printer.noSpaces.copy(dropNullValues = true)
}
