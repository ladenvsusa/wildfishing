package com.rivertrace.angleratlas;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Production seam for server-backed water level, weather and terrain data.
 * The demo deliberately ships without scraping logic inside the APK.
 */
public final class FishingDataSources {
    private FishingDataSources() {}

    public interface WaterLevelSource {
        WaterLevel latest(String stationCode);
    }

    public interface WeatherSource {
        Weather current(double latitude, double longitude);
    }

    public interface TerrainSource {
        TerrainProfile profile(String spotId);
    }

    public static final class WaterLevel {
        public final String stationCode;
        public final String stationName;
        public final double metres;
        public final double dailyChange;
        public final long measuredAt;

        public WaterLevel(String stationCode, String stationName, double metres, double dailyChange, long measuredAt) {
            this.stationCode = stationCode;
            this.stationName = stationName;
            this.metres = metres;
            this.dailyChange = dailyChange;
            this.measuredAt = measuredAt;
        }
    }

    public static final class Weather {
        public final String summary;
        public final double temperatureC;
        public final String wind;
        public final int pressureHpa;

        public Weather(String summary, double temperatureC, String wind, int pressureHpa) {
            this.summary = summary;
            this.temperatureC = temperatureC;
            this.wind = wind;
            this.pressureHpa = pressureHpa;
        }
    }

    /** Each point is [horizontal distance in metres, bed elevation in metres]. */
    public static final class TerrainProfile {
        public final String verticalDatum;
        public final List<double[]> points;

        public TerrainProfile(String verticalDatum, List<double[]> points) {
            this.verticalDatum = verticalDatum;
            this.points = Collections.unmodifiableList(points);
        }
    }

    public static final class DemoWaterLevelSource implements WaterLevelSource {
        @Override public WaterLevel latest(String stationCode) {
            return new WaterLevel(stationCode, "汉口水文站", 20.62, 0.00, System.currentTimeMillis());
        }
    }

    public static final class DemoWeatherSource implements WeatherSource {
        @Override public Weather current(double latitude, double longitude) {
            return new Weather("晴", 29, "东南风 2级", 1003);
        }
    }

    public static final class DemoTerrainSource implements TerrainSource {
        @Override public TerrainProfile profile(String spotId) {
            return new TerrainProfile("1985国家高程基准", Arrays.asList(
                new double[]{0, 20.4}, new double[]{18, 19.7}, new double[]{42, 17.9},
                new double[]{68, 17.6}, new double[]{94, 18.8}, new double[]{123, 19.2},
                new double[]{154, 18.4}, new double[]{188, 19.9}
            ));
        }
    }
}
