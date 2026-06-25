package com.lifeassistant.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 真正的向量 RAG 记忆搜索
 *
 * 嵌入模型: all-MiniLM-L6-v2 (本地 JVM 运行，384 维向量)
 * 向量库:   内存 ArrayList (无需 PostgreSQL)
 * 相似度:   余弦相似度
 *
 * 首次运行自动下载模型文件（~90MB），之后缓存本地
 */
@Service
public class MemorySearchService {

    private AllMiniLmL6V2EmbeddingModel embeddingModel;

    /** 内存向量库 */
    private final List<MemoryVector> vectorStore = new ArrayList<>();

    @PostConstruct
    public void init() {
        System.out.println("[RAG] 正在加载本地嵌入模型 all-MiniLM-L6-v2 ...");
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        System.out.println("[RAG] ✅ 嵌入模型就绪！向量维度: 384");
    }

    /**
     * 将一条对话记忆向量化后存入向量库
     */
    public void indexMemory(String userMessage, String aiReply) {
        if (embeddingModel == null) {
            System.err.println("[RAG] 模型未初始化，跳过索引");
            return;
        }

        String memoryText = "用户: " + userMessage + " | 助手: " + aiReply;

        // 向量化（384维 float 数组）
        Embedding embedding = embeddingModel.embed(memoryText).content();
        float[] vector = embedding.vector();

        vectorStore.add(new MemoryVector(vector, memoryText));
    }

    /**
     * 余弦相似度搜索
     */
    public String searchRelevantMemories(String query, int maxResults) {
        if (embeddingModel == null || vectorStore.isEmpty()) {
            return "";
        }

        // 1. 查询向量化
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 2. 计算每条记忆的余弦相似度
        List<ScoredMemory> scored = new ArrayList<>();
        for (MemoryVector mv : vectorStore) {
            double similarity = cosineSimilarity(queryEmbedding.vector(), mv.vector);
            if (similarity >= 0.3) { // 过滤低相似度
                scored.add(new ScoredMemory(mv.text, similarity));
            }
        }

        // 3. 按相似度降序，取前 N 条
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        StringBuilder result = new StringBuilder();
        int count = 0;
        for (ScoredMemory sm : scored) {
            if (count >= maxResults) break;
            result.append("- ").append(sm.text)
                  .append(" (相似度: ").append(String.format("%.2f", sm.score)).append(")\n");
            count++;
        }

        return result.toString();
    }

    /**
     * 余弦相似度: cos(θ) = A·B / (|A| × |B|)
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;

        double dotProduct = 0, magnitudeA = 0, magnitudeB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            magnitudeA += a[i] * a[i];
            magnitudeB += b[i] * b[i];
        }

        if (magnitudeA == 0 || magnitudeB == 0) return 0;
        return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }

    // ---- 内部类 ----

    /** 向量库中的一条记忆 */
    private record MemoryVector(float[] vector, String text) {}

    /** 搜索结果的评分记忆 */
    private record ScoredMemory(String text, double score) {}
}
