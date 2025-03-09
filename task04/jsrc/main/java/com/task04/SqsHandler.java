package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.SqsEvents;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import java.util.ArrayList;
import java.util.List;

@LambdaHandler(
	lambdaName = "sqs_handler", 
	roleName = "sqs_handler-role", 
	isPublishVersion = true, 
	aliasName = "${lambdas_alias_name}", 
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@SqsTriggerEventSource(targetQueue = "async_queue", batchSize = 123)
@SqsEvents
public class SqsHandler implements RequestHandler<SQSEvent, List<String>> {

	@Override
	public List<String> handleRequest(SQSEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("EVENT TYPE: " + event.getClass().toString());
		var messagesFound = new ArrayList<String>();
		for (SQSMessage msg : event.getRecords()) {
			messagesFound.add(msg.getBody());
		}
		return messagesFound;
	}
}
