package com.idea.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * LangChain4j wiring.
 *
 * OpenAI ChatModel and EmbeddingModel are auto-configured by
 * langchain4j-open-ai-spring-boot-starter from application.yml.
 * We only need to manually define the PgVectorEmbeddingStore bean.
 */
@Configuration
public class AiConfig {

    @Value("${app.ai.max-results:5}")
    private int maxResults;

    /**
     * PgVectorEmbeddingStore — backed by AWS RDS PostgreSQL + pgvector extension.
     * Uses the same DataSource as the rest of the app (no second connection pool).
     * Schema is managed by Flyway V2__pgvector.sql.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
        return PgVectorEmbeddingStore.datasourceBuilder()
            .datasource(dataSource)
            .table("vector_store")
            .dimension(1536)               // matches text-embedding-3-small
            .createTable(false)            // Flyway owns the schema
            .build();
    }
}
