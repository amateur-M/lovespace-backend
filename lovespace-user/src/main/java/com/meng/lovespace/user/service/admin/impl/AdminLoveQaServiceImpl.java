package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.entity.LoveQaConversation;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.mapper.LoveQaConversationMapper;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import com.meng.lovespace.user.service.admin.AdminLoveQaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminLoveQaServiceImpl implements AdminLoveQaService {

    private final LoveQaDocumentMapper documentMapper;
    private final LoveQaConversationMapper conversationMapper;
    private final ObjectProvider<LoveQaChatFacade> loveQaChatFacadeProvider;

    public AdminLoveQaServiceImpl(
            LoveQaDocumentMapper documentMapper,
            LoveQaConversationMapper conversationMapper,
            ObjectProvider<LoveQaChatFacade> loveQaChatFacadeProvider) {
        this.documentMapper = documentMapper;
        this.conversationMapper = conversationMapper;
        this.loveQaChatFacadeProvider = loveQaChatFacadeProvider;
    }

    @Override
    public IPage<LoveQaDocument> pageDocuments(
            String scope, String coupleId, long page, long pageSize) {
        LambdaQueryWrapper<LoveQaDocument> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(scope)) {
            qw.eq(LoveQaDocument::getScope, scope.trim());
        }
        if (StringUtils.hasText(coupleId)) {
            qw.eq(LoveQaDocument::getCoupleId, coupleId.trim());
        }
        qw.orderByDesc(LoveQaDocument::getUpdatedAt);
        return documentMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String adminUserId, String documentId) {
        LoveQaDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new ApiBusinessException(40400, "document not found");
        }
        LoveQaChatFacade facade = loveQaChatFacadeProvider.getIfAvailable();
        if (facade != null) {
            facade.deleteVectorsByDocumentId(doc.getDocumentId());
        }
        documentMapper.deleteById(doc.getDocumentId());
        log.info("admin.love-qa.deleteDocument adminUserId={} documentId={}", adminUserId, documentId);
    }

    @Override
    public IPage<LoveQaConversation> pageConversations(long page, long pageSize) {
        LambdaQueryWrapper<LoveQaConversation> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(LoveQaConversation::getUpdatedAt);
        return conversationMapper.selectPage(Page.of(page, pageSize), qw);
    }
}
