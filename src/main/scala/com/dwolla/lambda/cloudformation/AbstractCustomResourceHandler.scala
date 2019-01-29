package com.dwolla.lambda.cloudformation

import java.io._

import cats.data._
import cats.effect._
import cats.implicits._
import com.amazonaws.services.lambda.runtime._
import com.dwolla.lambda.cloudformation.AbstractCustomResourceHandler.stackTraceLines
import io.circe._
import io.circe.parser._
import org.apache.logging.log4j._

import scala.io.Source
import scala.language.higherKinds

abstract class AbstractCustomResourceHandler[F[_] : Effect] extends RequestStreamHandler {
  def handleRequest(req: CloudFormationCustomResourceRequest): F[HandlerResponse]

  private def readInputStream(inputStream: InputStream): EitherT[F, Throwable, String] =
    Async[F].delay(Source.fromInputStream(inputStream).mkString).attemptT

  private def parseStringLoggingErrors(str: String): EitherT[F, Throwable, Json] =
    EitherT.fromEither[F](parse(str)).leftSemiflatMap(ex ⇒ Async[F].delay {
      logger.error(s"Could not parse the following input:\n$str", ex)
      ex
    })

  /*_*/
  private def parseInputStream(inputStream: InputStream): EitherT[F, Throwable, CloudFormationCustomResourceRequest] =
    for {
      str ← readInputStream(inputStream)
      json ← parseStringLoggingErrors(str)
      req ← EitherT.fromEither[F](json.as[CloudFormationCustomResourceRequest]).leftWiden[Throwable]
    } yield req
  /*_*/

  private def handleRequestF(inputStream: InputStream): F[Unit] =
    parseInputStream(inputStream)
      .semiflatMap { req ⇒
        for {
          res ← handleRequest(req).attemptT.fold(exceptionResponse(req), successResponse(req))
          _ ← responseWriter.logAndWriteToS3(req.ResponseURL, res)
        } yield ()
      }
      .valueOrF { ex ⇒
        Async[F].delay(logger.error("failure", ex))
          .flatMap(_ ⇒ ex.raiseError[F, Unit])
      }

  //noinspection ScalaUnnecessaryParentheses
  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit =
    IO.async { cb: (Either[Throwable, Unit] ⇒ Unit) ⇒
      Effect[F].runAsync(handleRequestF(inputStream))(r ⇒ IO(cb(r))).unsafeRunSync()
    }.unsafeRunSync()

  protected def responseWriter: CloudFormationCustomResourceResponseWriter[F] = new CloudFormationCustomResourceResponseWriter[F]()

  protected lazy val logger: Logger = LogManager.getLogger("LambdaLogger")

  private def exceptionResponse(req: CloudFormationCustomResourceRequest)(ex: Throwable) = CloudFormationCustomResourceResponse(
    Status = RequestResponseStatus.Failed,
    Reason = Option(ex.getMessage),
    PhysicalResourceId = req.PhysicalResourceId,
    StackId = req.StackId,
    RequestId = req.RequestId,
    LogicalResourceId = req.LogicalResourceId,
    Data = JsonObject("StackTrace" → Json.arr(stackTraceLines(ex).map(Json.fromString): _*))
  )

  private def successResponse(req: CloudFormationCustomResourceRequest)(res: HandlerResponse) = CloudFormationCustomResourceResponse(
    Status = RequestResponseStatus.Success,
    Reason = None,
    PhysicalResourceId = Option(res.physicalId),
    StackId = req.StackId,
    RequestId = req.RequestId,
    LogicalResourceId = req.LogicalResourceId,
    Data = res.data
  )

}

object AbstractCustomResourceHandler {
  def stackTraceLines(throwable: Throwable): List[String] = {
    val writer = new StringWriter()
    throwable.printStackTrace(new PrintWriter(writer))
    writer.toString.lines.toList
  }
}
