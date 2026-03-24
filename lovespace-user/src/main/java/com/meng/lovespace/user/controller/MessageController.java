package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.MessageSendRequest;
import com.meng.lovespace.user.dto.PrivateMessageResponse;
import com.meng.lovespace.user.dto.ScheduledMessageCreateRequest;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 私密消息接口：发送、列表、已读、撤回、创建定时消息。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public ApiResponse<PrivateMessageResponse> send(Authentication auth, @Valid @RequestBody MessageSendRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("message.api.send senderId={} coupleId={} receiverId={}", p.userId(), req.coupleId(), req.receiverId());
        return ApiResponse.ok(messageService.send(p.userId(), req));
    }

    @GetMapping
    public ApiResponse<List<PrivateMessageResponse>> list(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("message.api.list userId={} coupleId={} page={} pageSize={}", p.userId(), coupleId, page, pageSize);
        return ApiResponse.ok(messageService.listMessages(p.userId(), coupleId, page, pageSize));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("message.api.read userId={} messageId={}", p.userId(), id);
        messageService.markRead(p.userId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/retract")
    public ApiResponse<Void> retract(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("message.api.retract userId={} messageId={}", p.userId(), id);
        messageService.retract(p.userId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/scheduled")
    public ApiResponse<PrivateMessageResponse> createScheduled(
            Authentication auth, @Valid @RequestBody ScheduledMessageCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info(
                "message.api.scheduled senderId={} coupleId={} receiverId={} scheduledTime={}",
                p.userId(),
                req.coupleId(),
                req.receiverId(),
                req.scheduledTime());
        return ApiResponse.ok(messageService.createScheduled(p.userId(), req));
    }
}
