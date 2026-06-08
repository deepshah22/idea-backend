package com.idea.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Wraps LangChain4j EmbeddingModel with an ElastiCache caching layer.
 *
 * Also handles ingestion of new documents into PgVectorEmbeddingStore.
 *
 * Cache key pattern : embed:{sha256 of text}
 * TTL               : 7 days (configurable)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.ai.embedding-cache-ttl-seconds:604800}")
    private long embeddingCacheTtlSeconds;

    /**
     * Embed text with ElastiCache caching.
     * Calls OpenAI only on a cache miss.
     */
    public float[] embed(String text) {
        String cacheKey = "embed:" + sha256(text);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Embedding cache hit for key {}", cacheKey);
            return deserialize(cached);
        }

        log.debug("Embedding cache miss — calling OpenAI");
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = embedding.vector();

        redisTemplate.opsForValue().set(
            cacheKey, serialize(vector), embeddingCacheTtlSeconds, TimeUnit.SECONDS
        );

        return vector;
    }

    /**
     * Ingest a document into pgvector.
     * Called when new content (career paths, offerings) is created or updated.
     */
    public void ingest(String id, String content, Map<String, Object> metadata) {
        var enrichedMetadata = new HashMap<>(metadata);
        enrichedMetadata.put("id", id);

        TextSegment segment = TextSegment.from(content, Metadata.from(enrichedMetadata));
        Embedding embedding = embeddingModel.embed(content).content();

        embeddingStore.addAll(List.of(embedding), List.of(segment));
        log.info("Ingested document id={}", id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String serialize(float[] v) {
        var sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.toString();
    }

    private float[] deserialize(String s) {
        String[] parts = s.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) v[i] = Float.parseFloat(parts[i]);
        return v;
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }

}
