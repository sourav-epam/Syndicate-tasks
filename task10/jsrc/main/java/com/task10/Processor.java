package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
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
public class Processor implements RequestHandler<Object, Map<String, Object>> {

	private static final String TABLE_NAME = System.getenv("target_table");

	private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();

	private static final ObjectMapper objectMapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public Map<String, Object> handleRequest(Object request, Context context) {

		try {
			double latitude = 50.4375;
			double longitude = 30.5;
			Weather weather = WeatherClient.fetchAndSaveWeather(latitude, longitude);
			// Serialize the Weather object into JSON
			String weatherJson = objectMapper.writeValueAsString(weather.getForecast());

			// Prepare the item to be inserted into DynamoDB
			Map<String, AttributeValue> item = new HashMap<>();

			item.put("id", new AttributeValue().withS(weather.getId()));
			item.put("forecast", new AttributeValue().withS(weatherJson));

			// Create the PutItemRequest
			PutItemRequest putItemRequest = new PutItemRequest()
					.withTableName(TABLE_NAME)
					.withItem(item);

			// Insert the item into DynamoDB
			client.putItem(putItemRequest);

			Map<String, Object> response = new HashMap<>();
			response.put("status", 201);
			response.put("message", "response saved to dynamodb");

			return response;
		} catch (Exception e) {
			throw new RuntimeException("Error saving weather data to DynamoDB: " + e.getMessage(), e);
		}
	}

	public static class WeatherClient {
		private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
		private static final OkHttpClient client = new OkHttpClient();

		public static Weather fetchAndSaveWeather(double latitude, double longitude) {
			try {
				// Fetch weather data from Open-Meteo API
				String apiResponse = getWeather(latitude, longitude);

				// Deserialize API response into Weather.Forecast object
				Weather.Forecast forecast = objectMapper.readValue(apiResponse, Weather.Forecast.class);

				// Create a Weather object
				Weather weather = new Weather(forecast);

				// Save the Weather object to DynamoDB
				return weather;

			} catch (Exception e) {
				throw new RuntimeException("Error fetching or saving weather data: " + e.getMessage(), e);
			}
		}

		public static String getWeather(double latitude, double longitude) throws IOException {
			HttpUrl url = HttpUrl.parse(BASE_URL)
					.newBuilder()
					.addQueryParameter("latitude", String.valueOf(latitude))
					.addQueryParameter("longitude", String.valueOf(longitude))
					.addQueryParameter("hourly", "temperature_2m,relative_humidity_2m,wind_speed_10m")
					.addQueryParameter("timezone", "auto")
					.build();

			Request request = new Request.Builder()
					.url(url)
					.get()
					.build();

			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					throw new IOException("Unexpected HTTP code " + response.code());
				}

				return response.body().string();
			}
		}
	}
}
