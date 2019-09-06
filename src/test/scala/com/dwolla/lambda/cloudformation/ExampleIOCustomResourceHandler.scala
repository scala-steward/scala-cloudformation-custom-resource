package com.dwolla.lambda.cloudformation

import cats.effect._

class ExampleIOCustomResourceHandler extends IOCustomResourceHandler {
  override def handleRequest(req: CloudFormationCustomResourceRequest): IO[HandlerResponse] = IO(HandlerResponse(tagPhysicalResourceId("phyiscal-resource-id")))
}

class ExampleFCustomResourceHandler[F[_] : Sync] extends AbstractCustomResourceHandler[F] {
  override def handleRequest(req: CloudFormationCustomResourceRequest): F[HandlerResponse] = Sync[F].delay {
    HandlerResponse(tagPhysicalResourceId("phyiscal-resource-id"))
  }
}
