package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.entity.LoveQaConversation;
import com.meng.lovespace.user.entity.LoveQaDocument;

/** 管理端恋爱问答服务。 */
public interface AdminLoveQaService {

    IPage<LoveQaDocument> pageDocuments(String scope, String coupleId, long page, long pageSize);

    void deleteDocument(String adminUserId, String documentId);

    IPage<LoveQaConversation> pageConversations(long page, long pageSize);
}
