package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;

import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

@LambdaHandler(lambdaName = "processor", roleName = "processor-role", isPublishVersion = true, aliasName = "${lambdas_alias_name}", logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED, tracingMode = TracingMode.Active)

@LambdaUrlConfig(authType = AuthType.NONE, invokeMode = InvokeMode.BUFFERED)

@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
})

@DependsOn(name = "Weather", resourceType = ResourceType.DYNAMODB_TABLE)
public class Processor implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private static final String TABLE_NAME = System.getenv("target_table");
	private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m";

	private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
	private static final OkHttpClient httpClient = new OkHttpClient();
	
	private static final ObjectMapper objectMapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	@Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            context.getLogger().log("Fetching weather data...");

            // Fetch weather forecast
            String weatherData = fetchWeatherData();
            Map<String, Object> weatherDataMap = objectMapper.readValue(weatherData, Map.class);

            // Create a new weather record
            Map<String, Object> forecast = new HashMap<>();
            forecast.put("elevation", weatherDataMap.get("elevation"));
            forecast.put("generationtime_ms", weatherDataMap.get("generationtime_ms"));
            forecast.put("hourly", weatherDataMap.get("hourly"));
            forecast.put("hourly_units", weatherDataMap.get("hourly_units"));
            forecast.put("latitude", weatherDataMap.get("latitude"));
            forecast.put("longitude", weatherDataMap.get("longitude"));
            forecast.put("timezone", weatherDataMap.get("timezone"));
            forecast.put("timezone_abbreviation", weatherDataMap.get("timezone_abbreviation"));
            forecast.put("utc_offset_seconds", weatherDataMap.get("utc_offset_seconds"));

            Map<String, Object> weatherRecord = new HashMap<>();
            weatherRecord.put("id", UUID.randomUUID().toString());
            weatherRecord.put("forecast", forecast);

            context.getLogger().log("Saving to DynamoDB: " + objectMapper.writeValueAsString(weatherRecord));

            // Save to DynamoDB
            saveToDynamoDB(weatherRecord);

            context.getLogger().log("Weather data saved successfully!");

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", 200);
            response.put("body", Map.of("message", "Weather data stored", "data", weatherRecord));

            return response;
        } catch (Exception e) {
            context.getLogger().log("Error fetching/storing weather data: " + e.getMessage());
            e.printStackTrace();

            // Return error response
            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", 500);
            response.put("body", Map.of("error", "Failed to fetch/store weather data"));
            return response;
        }
    }

    private String fetchWeatherData() throws Exception {
        Request request = new Request.Builder()
                .url(WEATHER_API_URL)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch weather data: HTTP " + response.code());
            }
            return response.body().string();
        }
    }

    private void saveToDynamoDB(Map<String, Object> weatherRecord) {
		// Map to store the DynamoDB item
		Map<String, AttributeValue> item = new HashMap<>();
	
		// Add the primary key (id)
		item.put("id", new AttributeValue().withS((String) weatherRecord.get("id")));
	
		// Convert the forecast object to JSON string and add it as an item
		try {
			String forecastJson = objectMapper.writeValueAsString(weatherRecord.get("forecast"));
			item.put("forecast", new AttributeValue().withS(forecastJson));
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize forecast to JSON: " + e.getMessage(), e);
		}
	
		// Create the PutItemRequest
		PutItemRequest putItemRequest = new PutItemRequest()
				.withTableName(TABLE_NAME) // DynamoDB table name
				.withItem(item);
	
		// Execute the PutItemRequest
		client.putItem(putItemRequest);
	}

}
