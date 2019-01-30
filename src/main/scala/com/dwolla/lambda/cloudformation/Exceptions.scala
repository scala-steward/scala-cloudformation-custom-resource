package com.dwolla.lambda.cloudformation

case class UnsupportedResourceType(resourceType: ResourceType)
  extends RuntimeException(s"""Unsupported resource type of "$resourceType".""")

case class UnsupportedRequestType(requestType: CloudFormationRequestType)
  extends RuntimeException(s"""Request Type "$requestType" not supported.""")

case class UnexpectedPhysicalId(physicalId: String)
  extends RuntimeException(s"A physical ID must not be provided, but received $physicalId")

case class MissingPhysicalId(requestType: CloudFormationRequestType)
  extends RuntimeException(s"A physical ID must be provided to execute $requestType")
