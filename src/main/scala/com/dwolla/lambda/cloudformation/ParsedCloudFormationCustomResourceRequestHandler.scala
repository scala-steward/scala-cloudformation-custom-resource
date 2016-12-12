package com.dwolla.lambda.cloudformation

import scala.concurrent.Future

trait ParsedCloudFormationCustomResourceRequestHandler {
  def handleRequest(input: CloudFormationCustomResourceRequest): Future[HandlerResponse]
  def shutdown(): Unit
}

case class HandlerResponse(physicalId: String,
                           data: Map[String, AnyRef] = Map.empty[String, AnyRef])
