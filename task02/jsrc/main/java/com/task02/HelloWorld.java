package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class HelloWorld implements RequestHandler<Object, Map<String, Object>> {

	public Map<String, Object> handleRequest(Object request, Context context) {
		System.out.println("Hello from lambda");
		Map<String, Object> event = (Map<String, Object>)request;
		 // Extract HTTP method and path from the event
		 String method = (String) event.getOrDefault("httpMethod", "UNKNOWN");
		 String path = (String) event.getOrDefault("path", "UNKNOWN");
 
		 // Check if the request is a GET request to /hello
		 if ("GET".equalsIgnoreCase(method) && "/hello".equals(path)) {
			 return createResponse(200, "Hello from Lambda");
		 }
 
		 // For all other requests, return a 400 Bad Request response
		 String errorMessage = String.format(
			 "Bad request syntax or unsupported method. Request path: %s. HTTP method: %s",
			 path, method
		 );
		 return createResponse(400, errorMessage);
	}

	private Map<String, Object> createResponse(int statusCode, String message) {
        return Map.of(
            "statusCode", statusCode,
            "headers", Map.of("Content-Type", "application/json"),
            "body", Map.of("message", message)
        );
    }
}