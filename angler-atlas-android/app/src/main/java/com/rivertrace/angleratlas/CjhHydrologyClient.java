package com.rivertrace.angleratlas;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the public real-time table embedded in http://www.cjh.com.cn/sqindex.html. */
public final class CjhHydrologyClient {
    public static final String SOURCE_URL = "http://www.cjh.com.cn/sqindex.html";
    private static final Pattern DATA = Pattern.compile("var\\s+sssq\\s*=\\s*(\\[.*?])\\s*;", Pattern.DOTALL);

    public static final class Station {
        public final String code;
        public final String name;
        public final String river;
        public final double level;
        public final String flow;
        public final String measuredAt;
        public final String trendCode;

        public Station(String code, String name, String river, double level,
                       String flow, String measuredAt, String trendCode) {
            this.code = code; this.name = name; this.river = river; this.level = level;
            this.flow = flow; this.measuredAt = measuredAt; this.trendCode = trendCode;
        }

        public String trendLabel() {
            if ("4".equals(trendCode)) return "回落";
            if ("5".equals(trendCode)) return "上涨";
            return "平稳";
        }
    }

    public interface Callback {
        void onLoaded(List<Station> stations);
        void onError(String message);
    }

    public void fetch(Callback callback) {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(SOURCE_URL).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("User-Agent", "AnglerAtlasBeta/0.4 (+personal fishing log)");
                connection.setRequestMethod("GET");
                StringBuilder html = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) html.append(line).append('\n');
                } finally {
                    connection.disconnect();
                }
                List<Station> stations = parse(html.toString());
                new Handler(Looper.getMainLooper()).post(() -> callback.onLoaded(stations));
            } catch (Exception error) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(error.getMessage()));
            }
        }, "cjh-water-level").start();
    }

    public static List<Station> parse(String html) throws Exception {
        Matcher matcher = DATA.matcher(html);
        if (!matcher.find()) throw new IllegalArgumentException("未找到实时水情表");
        JSONArray data = new JSONArray(matcher.group(1));
        List<Station> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject row = data.getJSONObject(i);
            if (!row.has("z")) continue;
            String flow = row.optString("q", "-");
            if (row.has("oq")) flow += "入 / " + row.optString("oq") + "出";
            result.add(new Station(row.optString("stcd"), row.optString("stnm"),
                row.optString("rvnm"), row.optDouble("z"), flow,
                row.optString("tm"), row.optString("wptn")));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("实时水情表为空");
        return result;
    }
}
