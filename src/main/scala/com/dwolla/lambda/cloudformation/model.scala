package com.dwolla.lambda.cloudformation

import io.circe.Json

case class CloudFormationCustomResourceRequest(RequestType: String,
                                               ResponseURL: String,
                                               StackId: String,
                                               RequestId: String,
                                               ResourceType: String,
                                               LogicalResourceId: String,
                                               PhysicalResourceId: Option[String],
                                               ResourceProperties: Option[Map[String, Json]],
                                               OldResourceProperties: Option[Map[String, Json]])

case class CloudFormationCustomResourceResponse(Status: String,
                                                Reason: Option[String],
                                                PhysicalResourceId: Option[String],
                                                StackId: String,
                                                RequestId: String,
                                                LogicalResourceId: String,
                                                Data: Map[String, Json] = Map.empty[String, Json])

object MissingResourceProperties extends RuntimeException
