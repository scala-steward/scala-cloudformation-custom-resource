package com.dwolla.lambda.cloudformation

import org.json4s.JValue

case class CloudFormationCustomResourceRequest(RequestType: String,
                                               ResponseURL: String,
                                               StackId: String,
                                               RequestId: String,
                                               ResourceType: String,
                                               LogicalResourceId: String,
                                               PhysicalResourceId: Option[String],
                                               ResourceProperties: Option[Map[String, JValue]],
                                               OldResourceProperties: Option[Map[String, JValue]])

case class CloudFormationCustomResourceResponse(Status: String,
                                                Reason: Option[String],
                                                PhysicalResourceId: Option[String],
                                                StackId: String,
                                                RequestId: String,
                                                LogicalResourceId: String,
                                                Data: Map[String, AnyRef] = Map.empty[String, AnyRef])

object MissingResourceProperties extends RuntimeException
