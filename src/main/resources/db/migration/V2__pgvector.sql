-- V2__pgvector.sql
-- Enable pgvector extension for semantic search.
-- Requires pgvector installed on RDS:
--   AWS RDS PostgreSQL 15+ supports pgvector natively.
--   Enable via: AWS Console → RDS → Parameter Groups → pgvector

CREATE EXTENSION IF NOT EXISTS vector;

-- Spring AI pgvector store creates its own table automatically,
-- but we define it explicitly here so Flyway owns the schema.
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding vector(1536)          -- matches text-embedding-3-small dimensions
);

-- IVFFlat index for fast approximate nearest-neighbour search.
-- lists = sqrt(row_count) is a good starting point.
-- Rebuild this index after bulk-loading embeddings.
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
