package com.meng.lovespace.user.task;

import com.meng.lovespace.user.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时派发到期私密消息。
 */
@Slf4j
@Component
public class ScheduledMessageDispatchTask {

    private final MessageService messageService;

    public ScheduledMessageDispatchTask(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 每 15 秒扫描一次到期定时消息并派发。
     */
    @Scheduled(fixedDelay = 15000)
    public void dispatchDueMessages() {
        int dispatched = messageService.dispatchDueScheduledMessages();
        if (dispatched > 0) {
            log.info("message.task.dispatched count={}", dispatched);
        }
    }
}
