package com.dwolla.lambda.cloudformation

import io.circe.Json

case class HandlerResponse(physicalId: String,
                           data: Map[String, Json] = Map.empty[String, Json])
