package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

import java.util.ArrayList;
import java.util.List;

@LambdaHandler(lambdaName = "sns_handler", roleName = "sns_handler-role", isPublishVersion = true, aliasName = "${lambdas_alias_name}", logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED)

@SnsEventSource(targetTopic = "lambda_topic")
public class SnsHandler implements RequestHandler<SNSEvent, List<String>> {

	@Override
	public List<String> handleRequest(SNSEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("EVENT TYPE: " + event.getClass().toString());
		var messagesFound = new ArrayList<String>();
		for (SNSRecord record : event.getRecords()) {
			SNS message = record.getSNS();
			messagesFound.add(message.getMessage());
		}
		return messagesFound;
	}
}
