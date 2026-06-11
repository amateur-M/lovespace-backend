package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoveQaDocumentDetail;
import com.meng.lovespace.user.dto.LoveQaDocumentPageResponse;
import com.meng.lovespace.user.dto.LoveQaIngestResponseData;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveQaDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 恋爱知识库文档台账：列表、详情、删除与重入库。 */
@Tag(name = "AI", description = "恋爱知识库文档台账")
@Validated
@RestController
@RequestMapping("/api/v1/ai/love-qa/documents")
@RequiredArgsConstructor
public class LoveQaDocumentController {

    private final LoveQaDocumentService loveQaDocumentService;

    @Operation(summary = "知识库文档分页列表")
    @GetMapping
    public ApiResponse<LoveQaDocumentPageResponse> listDocuments(
            Authentication auth,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveQaDocumentService.pageDocuments(p.userId(), coupleId, page, pageSize));
    }

    @Operation(summary = "知识库文档详情")
    @GetMapping("/{documentId}")
    public ApiResponse<LoveQaDocumentDetail> getDocument(
            Authentication auth, @PathVariable("documentId") String documentId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveQaDocumentService.getDocument(p.userId(), documentId));
    }

    @Operation(summary = "删除知识库文档（Milvus 向量 + 台账）")
    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> deleteDocument(
            Authentication auth, @PathVariable("documentId") String documentId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        loveQaDocumentService.deleteDocument(p.userId(), documentId);
        return ApiResponse.ok();
    }

    @Operation(summary = "强制重入库（删旧向量后按内容快照重新分片入库）")
    @PostMapping("/{documentId}/reingest")
    public ApiResponse<LoveQaIngestResponseData> reingest(
            Authentication auth, @PathVariable("documentId") String documentId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveQaDocumentService.reingest(p.userId(), documentId));
    }
}
