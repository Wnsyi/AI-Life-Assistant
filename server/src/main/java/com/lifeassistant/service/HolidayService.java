package com.lifeassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeassistant.model.Holiday;
import com.lifeassistant.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 节假日服务 — 实时 API 优先 + 数据库兜底
 *
 * 这才是真正的 Agent 工具：数据源是活的
 */
@Service
public class HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);

    @Autowired
    private HolidayRepository holidayRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 查询节假日 — API 优先
     */
    public String checkHoliday(String dateStr) {
        LocalDate date = parseDate(dateStr);

        // 1. 尝试调用实时 API
        String apiResult = queryApi(date);
        if (apiResult != null) {
            return apiResult; // ✅ 活数据
        }

        // 2. API 挂了 → 降级查数据库
        String dbResult = queryDatabase(date);
        if (dbResult != null) {
            return dbResult; // ⚠ 缓存数据
        }

        // 3. 都不行 → 简单判断周末
        return fallbackWeekend(date);
    }

    // ==================== 数据源1: 实时 API ====================

    private String queryApi(LocalDate date) {
        try {
            String url = "https://api.haoshenqi.top/holiday?date=" + date;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET()
                    .timeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> resp = httpClient.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return null;

            JsonNode root = mapper.readTree(resp.body());
            if (!root.isArray() || root.isEmpty()) return null;

            int status = root.get(0).path("status").asInt(-1);

            return switch (status) {
                case 3 -> "🎉 " + date + " 是休息日，放假！";
                case 2 -> "⚠ " + date + " 是调休上班日！虽然是周末但需要上班。";
                case 0 -> date + " 是普通工作日。";
                default -> null;
            };

        } catch (Exception e) {
            log.warn("API 调用失败，降级到数据库: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 数据源2: MySQL 数据库 ====================

    private String queryDatabase(LocalDate date) {
        Optional<Holiday> opt = holidayRepository.findByHolidayDate(date);
        if (opt.isEmpty()) return null;

        Holiday h = opt.get();
        if (Boolean.TRUE.equals(h.getIsWorkday())) {
            return "⚠ " + date + " 是调休上班日（" + h.getHolidayName() + "）。";
        }
        if (Boolean.TRUE.equals(h.getIsHoliday())) {
            return "🎉 " + date + " 是" + h.getHolidayName() + "，放假！";
        }
        return null;
    }

    // ==================== 数据源3: 纯计算 ====================

    private String fallbackWeekend(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        if (dow >= 6) return date + " 是周末，正常休息。";
        return date + " 是工作日。";
    }

    private LocalDate parseDate(String s) {
        s = s.trim();
        try {
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(s);
            if (s.matches("\\d{4}\\d{2}\\d{2}"))
                return LocalDate.parse(s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6,8));
            if (s.matches("\\d{2}-\\d{2}"))
                return LocalDate.of(LocalDate.now().getYear(),
                        Integer.parseInt(s.substring(0,2)), Integer.parseInt(s.substring(3,5)));
        } catch (Exception ignored) {}
        return LocalDate.now();
    }
}
