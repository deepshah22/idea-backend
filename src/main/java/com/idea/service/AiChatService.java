package com.idea.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Orchestrates the full RAG (Retrieval-Augmented Generation) flow
 * using LangChain4j:
 *
 *  1. Check ElastiCache — return immediately on hit
 *  2. Embed the query via OpenAI (with caching)
 *  3. Semantic search via PgVectorEmbeddingStore
 *  4. Build grounded prompt from retrieved documents
 *  5. Call ChatLanguageModel (OpenAI)
 *  6. Cache response in ElastiCache
 *  7. Return answer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.ai.cache-ttl-seconds:86400}")
    private long cacheTtlSeconds;

    @Value("${app.ai.similarity-threshold:0.70}")
    private double similarityThreshold;

    @Value("${app.ai.max-results:5}")
    private int maxResults;

    private static final String SYSTEM_PROMPT = """
        You are a helpful career guidance assistant.
        You help families find age-appropriate activities for children
        pursuing their dream career.
        Be encouraging, concise, and specific.
        Only recommend activities from the context provided.
        Never invent courses, camps, or events that are not in the context.
        Respond in plain text, no markdown.
        """;

    /**
     * Main entry point — question + contextId (e.g. userId or childId for cache namespacing).
     */
    public String chat(String question, String contextId) {

        // 1. cache check
        String cacheKey = "ai:chat:" + contextId + ":" + sha256(question);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("AI response cache hit");
            return cached;
        }

        // 2. embed the query
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        // 3. semantic search via pgvector
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(similarityThreshold)
                .build()
        ).matches();

        log.debug("pgvector returned {} matches above threshold {}", matches.size(), similarityThreshold);

        // 4. build grounded prompt
        String userPrompt = buildPrompt(question, matches);

        // 5. call LLM via LangChain4j
        String response = chatModel.generate(
            List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(userPrompt)
            )
        ).content().text();

        // 6. cache and return
        redisTemplate.opsForValue().set(cacheKey, response, cacheTtlSeconds, TimeUnit.SECONDS);
        return response;
    }

    // ── private ──────────────────────────────────────────────────────────────

    private String buildPrompt(String question, List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) {
            return question + "\n\n(No relevant context found in the database.)";
        }

        String context = matches.stream()
            .map(m -> m.embedded().text())
            .collect(Collectors.joining("\n---\n"));

        return """
            Use only the following context to answer the question.
            If the answer is not in the context, say so honestly.

            CONTEXT:
            %s

            QUESTION:
            %s
            """.formatted(context, question);
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

    public void ingest(String id, String content, Map<String, Object> metadata) {
        TextSegment segment = TextSegment.from(content, Metadata.from(metadata));
        Embedding embedding = embeddingModel.embed(content).content();
        embeddingStore.addAll(List.of(embedding), List.of(segment));
    }
}
