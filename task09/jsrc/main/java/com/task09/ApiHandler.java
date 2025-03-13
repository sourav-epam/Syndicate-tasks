package com.task09;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
	lambdaName = "api_handler", 
	roleName = "api_handler-role", 
	isPublishVersion = true, 
	aliasName = "${lambdas_alias_name}", 
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
	layerName = "weather_sdk", 
	libraries = {
		"lib/sdk-1.10.0.jar" }, 
		runtime = DeploymentRuntime.JAVA11, 
		artifactExtension = ArtifactExtension.ZIP
)
		
@LambdaUrlConfig(
	authType = AuthType.NONE, 
	invokeMode = InvokeMode.BUFFERED
)
@SuppressWarnings("unchecked")
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

	public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {

		String path = (String) event.get("rawPath");
        Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
        Map<String, Object> http = (Map<String, Object>) requestContext.get("http");
        String method = (String) http.get("method");

		if (!"/weather".equals(path) || !"GET".equalsIgnoreCase(method)) {
            return createResponse(400, Map.of(
                    "statusCode", 400,
                    "message", String.format("Bad request syntax or unsupported method. Request path: %s. HTTP method: %s", path, method)
            ));
        }

        try {
            WeatherClient weatherClient = new WeatherClient();
            Map<String, Object> weatherData = weatherClient.getWeather(50.4375, 30.5);

            return createResponse(200, weatherData);
        } catch (Exception e) {
            context.getLogger().log("Error fetching weather data: " + e.getMessage());
            return createResponse(500, Map.of(
                    "error", "Internal Server Error"
            ));
        }
	}

	private Map<String, Object> createResponse(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        try {
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (IOException e) {
            response.put("body", "{\"error\": \"Failed to serialize response body\"}");
        }
        response.put("headers", Map.of("content-type", "application/json"));
        response.put("isBase64Encoded", false);
        return response;
    }

	static class WeatherClient {

		public Map<String, Object> getWeather(double latitude, double longitude) throws IOException {
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

            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected HTTP code " + response.code());
                }

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.body().string(), Map.class);
            }
        }
    }
}
