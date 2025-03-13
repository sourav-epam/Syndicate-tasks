package com.task10;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class Weather {

    private String id; // UUIDv4

    @JsonProperty("forecast")
    private Forecast forecast;

    // Default constructor
    public Weather() {
    }

    // Constructor to initialize Weather
    public Weather(Forecast forecast) {
        this.id = UUID.randomUUID().toString(); // Automatically generate unique ID
        this.forecast = forecast;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
    }

    // Nested class for Forecast
    public static class Forecast {
        private double elevation;
        private double generationtime_ms;

        @JsonProperty("hourly")
        private Hourly hourly;

        @JsonProperty("hourly_units")
        private HourlyUnits hourlyUnits;

        private double latitude;
        private double longitude;
        private String timezone;
        private String timezone_abbreviation;

        @JsonProperty("utc_offset_seconds")
        private int utcOffsetSeconds;

        // Getters and Setters
        public double getElevation() {
            return elevation;
        }

        public void setElevation(double elevation) {
            this.elevation = elevation;
        }

        public double getGenerationtime_ms() {
            return generationtime_ms;
        }

        public void setGenerationtime_ms(double generationtime_ms) {
            this.generationtime_ms = generationtime_ms;
        }

        public Hourly getHourly() {
            return hourly;
        }

        public void setHourly(Hourly hourly) {
            this.hourly = hourly;
        }

        public HourlyUnits getHourlyUnits() {
            return hourlyUnits;
        }

        public void setHourlyUnits(HourlyUnits hourlyUnits) {
            this.hourlyUnits = hourlyUnits;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getTimezone_abbreviation() {
            return timezone_abbreviation;
        }

        public void setTimezone_abbreviation(String timezone_abbreviation) {
            this.timezone_abbreviation = timezone_abbreviation;
        }

        public int getUtcOffsetSeconds() {
            return utcOffsetSeconds;
        }

        public void setUtcOffsetSeconds(int utcOffsetSeconds) {
            this.utcOffsetSeconds = utcOffsetSeconds;
        }
    }

    // Nested class for Hourly
    public static class Hourly {
        private List<Double> temperature_2m;
        private List<String> time;

        // Getters and Setters
        public List<Double> getTemperature_2m() {
            return temperature_2m;
        }

        public void setTemperature_2m(List<Double> temperature_2m) {
            this.temperature_2m = temperature_2m;
        }

        public List<String> getTime() {
            return time;
        }

        public void setTime(List<String> time) {
            this.time = time;
        }
    }

    // Nested class for HourlyUnits
    public static class HourlyUnits {
        private String temperature_2m;
        private String time;

        // Getters and Setters
        public String getTemperature_2m() {
            return temperature_2m;
        }

        public void setTemperature_2m(String temperature_2m) {
            this.temperature_2m = temperature_2m;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }

}
