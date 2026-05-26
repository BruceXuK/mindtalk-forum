-- 敏感词过滤
CREATE TABLE IF NOT EXISTS sensitive_words (
    id          BIGSERIAL PRIMARY KEY,
    word        VARCHAR(100) NOT NULL UNIQUE,
    replacement VARCHAR(50) DEFAULT '***',
    category    VARCHAR(30) DEFAULT 'general',
    enabled     BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE sensitive_words IS '敏感词库';
COMMENT ON COLUMN sensitive_words.word IS '敏感词';
COMMENT ON COLUMN sensitive_words.replacement IS '替换文本';

CREATE UNIQUE INDEX uk_sw_word ON sensitive_words(word) WHERE deleted = 0;
