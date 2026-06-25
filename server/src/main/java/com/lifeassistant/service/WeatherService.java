package com.lifeassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 天气服务
 * - 实时天气: wttr.in (j1)
 * - 天气预报: Open-Meteo (免费，无需 Key)
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<Integer, String> WMO_CODES = Map.ofEntries(
            Map.entry(0, "晴天"), Map.entry(1, "大部晴朗"), Map.entry(2, "多云"),
            Map.entry(3, "阴天"), Map.entry(45, "有雾"), Map.entry(48, "雾凇"),
            Map.entry(51, "小毛毛雨"), Map.entry(53, "毛毛雨"), Map.entry(55, "大毛毛雨"),
            Map.entry(61, "小雨"), Map.entry(63, "中雨"), Map.entry(65, "大雨"),
            Map.entry(71, "小雪"), Map.entry(73, "中雪"), Map.entry(75, "大雪"),
            Map.entry(80, "小阵雨"), Map.entry(81, "中阵雨"), Map.entry(82, "大阵雨"),
            Map.entry(85, "小阵雪"), Map.entry(86, "大阵雪"),
            Map.entry(95, "雷暴"), Map.entry(96, "冰雹雷暴"), Map.entry(99, "强雷暴")
    );

    /** 实时天气 — wttr.in */
    public String getWeather(String city) {
        try {
            String url = "https://wttr.in/" + encode(city) + "?format=j1";
            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder().uri(URI.create(url)).GET()
                            .timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return "天气查询失败";

            JsonNode root = mapper.readTree(resp.body());
            JsonNode cur = root.path("current_condition").get(0);
            JsonNode near = root.path("nearest_area").get(0);

            return String.format("""
                    🌍 %s, %s 当前天气：
                    🌤 %s  🌡 %s°C（体感 %s°C）
                    💧 湿度 %s%%  🌬 %s %s km/h  👁 能见度 %s km
                    💡 如需未来几天预报，请说"明天天气\"""",
                    near.path("areaName").get(0).path("value").asText(),
                    near.path("country").get(0).path("value").asText(),
                    cur.path("weatherDesc").get(0).path("value").asText(),
                    cur.path("temp_C").asText(), cur.path("FeelsLikeC").asText(),
                    cur.path("humidity").asText(),
                    cur.path("winddir16Point").asText(), cur.path("windspeedKmph").asText(),
                    cur.path("visibility").asText());
        } catch (Exception e) {
            log.warn("天气查询失败: {}", e.getMessage());
            return "天气查询失败: " + e.getMessage();
        }
    }

    /** 3天预报 — wttr.in 翻译城市名 + Open-Meteo 预报 */
    public String getForecast(String city) {
        try {
            // 1. 用 wttr.in 获取城市的英文名（支持中文城市名）
            String wttrUrl = "https://wttr.in/" + encode(city) + "?format=j1";
            HttpResponse<String> wttrResp = httpClient.send(
                    HttpRequest.newBuilder().uri(URI.create(wttrUrl)).GET()
                            .timeout(Duration.ofSeconds(8)).build(),
                    HttpResponse.BodyHandlers.ofString());

            String engCity = city; // 默认用原名
            if (wttrResp.statusCode() == 200) {
                JsonNode wttrRoot = mapper.readTree(wttrResp.body());
                JsonNode nearest = wttrRoot.path("nearest_area");
                if (nearest.isArray() && nearest.size() > 0) {
                    engCity = nearest.get(0).path("areaName").get(0).path("value").asText();
                }
            }

            // 2. Open-Meteo 地理编码（用英文名）
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + encode(engCity) + "&count=1";
            HttpResponse<String> geoResp = httpClient.send(
                    HttpRequest.newBuilder().uri(URI.create(geoUrl)).GET()
                            .timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (geoResp.statusCode() != 200) return "城市未找到: " + city;
            JsonNode geo = mapper.readTree(geoResp.body());
            if (!geo.has("results") || geo.path("results").isEmpty())
                return "未找到城市「" + city + "」，请尝试用拼音或英文名";

            JsonNode loc = geo.path("results").get(0);
            double lat = loc.path("latitude").asDouble();
            double lon = loc.path("longitude").asDouble();
            String cityName = loc.path("name").asText();

            // 3. 预报
            String fcUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                    + "&daily=temperature_2m_max,temperature_2m_min,weathercode"
                    + "&timezone=Asia/Shanghai&forecast_days=3", lat, lon);
            HttpResponse<String> fcResp = httpClient.send(
                    HttpRequest.newBuilder().uri(URI.create(fcUrl)).GET()
                            .timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString());

            JsonNode fc = mapper.readTree(fcResp.body());
            JsonNode daily = fc.path("daily");

            StringBuilder sb = new StringBuilder("🌍 ").append(cityName).append(" 天气预报：\n");
            String[] labels = {"今天", "明天", "后天"};

            for (int i = 0; i < 3 && i < daily.path("time").size(); i++) {
                String date = daily.path("time").get(i).asText();
                int max = daily.path("temperature_2m_max").get(i).asInt();
                int min = daily.path("temperature_2m_min").get(i).asInt();
                int code = daily.path("weathercode").get(i).asInt();
                String desc = WMO_CODES.getOrDefault(code, "未知");
                sb.append(String.format("%s（%s）：🌤 %s  🌡 %d°C ~ %d°C\n",
                        labels[i], date, desc, min, max));
            }
            return sb.toString();

        } catch (Exception e) {
            log.warn("预报失败: {}", e.getMessage());
            return "天气预报查询失败: " + e.getMessage();
        }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
