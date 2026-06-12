-- 恋爱知识库文档 FULLTEXT 索引（P2-C 混合检索：title + category + content，ngram 支持中文）
-- 存量库执行；新库见 love_qa_documents.sql
ALTER TABLE love_qa_documents
  ADD FULLTEXT INDEX ft_love_qa_doc_search (title, category, content) WITH PARSER ngram;
