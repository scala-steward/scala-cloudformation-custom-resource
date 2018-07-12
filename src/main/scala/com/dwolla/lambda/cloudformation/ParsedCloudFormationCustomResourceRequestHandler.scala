package com.dwolla.lambda.cloudformation

import io.circe.Json

import scala.concurrent.Future

@deprecated("implement handleRequest in CatsAbstractCustomResourceHandler instead", since = "2.0.0")
trait ParsedCloudFormationCustomResourceRequestHandler {
  def handleRequest(input: CloudFormationCustomResourceRequest): Future[HandlerResponse]
  def shutdown(): Unit
}

case class HandlerResponse(physicalId: String,
                           data: Map[String, Json] = Map.empty[String, Json])
