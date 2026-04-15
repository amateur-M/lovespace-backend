package com.meng.lovespace.ai.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Redis 中持久化的会话状态（按用户隔离，可选绑定 coupleId）。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoveQAConversationState {

    private String userId;
    /** 可选：首次请求传入后固化，用于同一会话内上下文一致。 */
    private String coupleId;

    private List<LoveQAConversationTurn> turns = new ArrayList<>();
}
