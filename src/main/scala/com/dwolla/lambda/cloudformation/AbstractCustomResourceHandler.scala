package com.dwolla.lambda.cloudformation

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import org.json4s.ParserUtil.ParseException
import org.json4s._
import org.json4s.native.Serialization._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.Try

abstract class AbstractCustomResourceHandler extends RequestStreamHandler {
  def createParsedRequestHandler(): ParsedCloudFormationCustomResourceRequestHandler

  implicit val executionContext: ExecutionContext

  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    val input = Source.fromInputStream(inputStream).mkString

    val req = try {
      read[CloudFormationCustomResourceRequest](input)
    } catch {
      case ex: ParseException ⇒
        logger.error(input, ex)
        throw ex
    }

    logger.debug(write(req))

    val actualHandler = createParsedRequestHandler()

    val eventuallyHandledRequest = Try(actualHandler.handleRequest(req))
      .recover {
        case ex ⇒ Future.failed(ex)
      }
      .get
      .map { res ⇒
        CloudFormationCustomResourceResponse(
          Status = "SUCCESS",
          Reason = None,
          PhysicalResourceId = Option(res.physicalId),
          StackId = req.StackId,
          RequestId = req.RequestId,
          LogicalResourceId = req.LogicalResourceId,
          Data = res.data
        )
      }.recover {
      case ex: Exception ⇒
        CloudFormationCustomResourceResponse(
          Status = "FAILED",
          Reason = Option(ex.getMessage),
          PhysicalResourceId = req.PhysicalResourceId,
          StackId = req.StackId,
          RequestId = req.RequestId,
          LogicalResourceId = req.LogicalResourceId
        )
    }.flatMap { res ⇒
      responseWriter.logAndWriteToS3(req.ResponseURL, res)
    }

    try {
      Await.ready(eventuallyHandledRequest, 30 seconds)
    } finally {
      actualHandler.shutdown()
    }
  }

  protected def responseWriter: CloudFormationCustomResourceResponseWriter = new CloudFormationCustomResourceResponseWriter()

  protected lazy val logger: Logger = LoggerFactory.getLogger("LambdaLogger")

  protected implicit val formats: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
}
