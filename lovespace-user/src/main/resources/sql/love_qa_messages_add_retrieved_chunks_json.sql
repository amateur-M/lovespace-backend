-- 恋爱问答 assistant 消息持久化检索引用快照（P2-E / Sprint 4）
ALTER TABLE love_qa_messages
  ADD COLUMN retrieved_chunks_json MEDIUMTEXT NULL COMMENT 'assistant 消息的 RetrievedChunk JSON 快照' AFTER content;
