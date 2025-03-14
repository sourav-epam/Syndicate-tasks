package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.TracingMode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "processor", roleName = "processor-role", isPublishVersion = true, aliasName = "${lambdas_alias_name}", tracingMode = TracingMode.Active, logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED)
@LambdaUrlConfig
@EnvironmentVariables(value = { @EnvironmentVariable(key = "target_table", value = "${target_table}") })
public class Processor implements RequestHandler<Object, Map<String, Object>> {
	private final String url = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m";
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
	private final DynamoDB dynamoDB = new DynamoDB(client);
	private final Table weatherTable = dynamoDB.getTable(System.getenv("target_table"));

	public Map<String, Object> handleRequest(Object request, Context context) {
		Map<String, Object> input = null;
		try {
			input = input();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
		Map<String, List<?>> hourly = new HashMap<>();
		hourly.put("temperature_2m", ((Map<String, List<?>>) input.get("hourly")).get("temperature_2m"));
		hourly.put("time", (((Map<String, List<?>>) input.get("hourly")).get("time")));
		Map<String, String> hourly_units = new HashMap<>();
		hourly_units.put("temperature_2m", ((Map<String, String>) input.get("hourly_units")).get("temperature_2m"));
		hourly_units.put("time", ((Map<String, String>) input.get("hourly_units")).get("time"));
		Map<String, Object> forecast = new HashMap<>();
		forecast.put("elevation", (Number) input.get("elevation"));
		forecast.put("generationtime_ms", (Number) input.get("generationtime_ms"));
		forecast.put("hourly", hourly);
		forecast.put("hourly_units", hourly_units);
		forecast.put("latitude", (Number) input.get("latitude"));
		forecast.put("longitude", (Number) input.get("longitude"));
		forecast.put("timezone", (String) input.get("timezone"));
		forecast.put("timezone_abbreviation", (String) input.get("timezone_abbreviation"));
		forecast.put("utc_offset_seconds", (Number) input.get("utc_offset_seconds"));
		Item item = new Item().withPrimaryKey("id", UUID.randomUUID().toString()).withMap("forecast", forecast);
		weatherTable.putItem(item);
		System.out.println("Hello from lambda");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("statusCode", 200);
		resultMap.put("body", "Hello from Lambda");
		return resultMap;
	}

	private Map<String, Object> input() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		return objectMapper.readValue(response.body(), Map.class);
	}
}