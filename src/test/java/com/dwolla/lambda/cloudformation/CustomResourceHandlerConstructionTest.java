package com.dwolla.lambda.cloudformation;

import cats.effect.IO;
import scala.Option;

public class CustomResourceHandlerConstructionTest {

    public static void main(String[] args) {
        final ExampleIOCustomResourceHandler x = new ExampleIOCustomResourceHandler(); // no argument constructor is required for Lambda to work!

        final IO<HandlerResponse> handlerResponseIO = x.handleRequest(CloudFormationCustomResourceRequest.apply(CloudFormationRequestType.CreateRequest$.MODULE$, "", "", "", "", "", Option.apply(null), Option.apply(null), Option.apply(null)));
        final HandlerResponse handlerResponse = handlerResponseIO.unsafeRunSync();

        System.out.println(handlerResponse);
    }

}
