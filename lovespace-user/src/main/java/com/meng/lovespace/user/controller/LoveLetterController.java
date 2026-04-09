package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoveLetterGenerateRequest;
import com.meng.lovespace.user.dto.LoveLetterResponseData;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 情书生成。 */
@Tag(name = "AI Love Letter", description = "情书生成")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class LoveLetterController {

    private final LoveLetterService loveLetterService;

    @Operation(summary = "生成情书")
    @PostMapping("/love-letter")
    public ApiResponse<LoveLetterResponseData> generate(
            Authentication auth, @Valid @RequestBody LoveLetterGenerateRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        String content =
                loveLetterService.generate(
                        p.userId(), request.coupleId(), request.style(), request.length(), request.memories());
        return ApiResponse.ok(new LoveLetterResponseData(content));
    }
}
