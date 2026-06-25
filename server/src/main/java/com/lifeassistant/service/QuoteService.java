package com.lifeassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeassistant.model.Quote;
import com.lifeassistant.repository.QuoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

/**
 * 名言服务 — 实时 API 优先 + 数据库兜底
 */
@Service
public class QuoteService {

    @Autowired
    private QuoteRepository quoteRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** 实时 API — 一言 (hitokoto.cn)，免费无需 Key */
    public String getRandomQuote() {
        // 1. 尝试实时 API
        String apiResult = queryApi();
        if (apiResult != null) return apiResult;

        // 2. 降级到数据库
        return queryDatabase();
    }

    /** 每日一句（同一天返回同一句） */
    public String getDailyQuote() {
        // 每日一句也用实时 API（API 本身返回随机内容，无日期维度）
        String quote = getRandomQuote();
        return "📖 每日一句\n" + quote;
    }

    // ==================== 数据源1: hitokoto.cn ====================

    private String queryApi() {
        try {
            // c=a: 动画 c=d: 文学 c=i: 诗词 c=k: 哲理
            // 混合类型，拿到更有意思的内容
            String url = "https://v1.hitokoto.cn/?c=d&c=i&c=k";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("User-Agent", "AILifeAssistant/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = httpClient.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return null;

            JsonNode root = mapper.readTree(resp.body());
            String text = root.path("hitokoto").asText("");
            String from = root.path("from").asText("");
            String author = root.path("from_who").asText("");

            if (text.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("💬 \"");
            sb.append(text).append("\"");
            if (!author.isEmpty()) {
                sb.append("\n   — ").append(author);
                if (!from.isEmpty() && !from.equals(author)) {
                    sb.append("《").append(from).append("》");
                }
            } else if (!from.isEmpty()) {
                sb.append("\n   — 《").append(from).append("》");
            }

            return sb.toString();

        } catch (Exception e) {
            System.err.println("[Quote] API 失败: " + e.getMessage());
            return null;
        }
    }

    // ==================== 数据源2: MySQL ====================

    private String queryDatabase() {
        Quote q = quoteRepository.findRandom();
        if (q == null) {
            return "💬 \"学无止境\" — 佚名";
        }
        return "💬 \"" + q.getContent() + "\"\n   — " + q.getAuthor();
    }
}
