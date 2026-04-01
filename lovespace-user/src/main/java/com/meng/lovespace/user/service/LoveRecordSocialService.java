package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.LoveRecordCommentCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordCommentPageResponse;
import com.meng.lovespace.user.dto.LoveRecordCommentResponse;
import com.meng.lovespace.user.dto.LoveRecordLikeStateResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.entity.LoveRecord;
import java.util.List;

/**
 * 恋爱记录互动：点赞、评论；与 {@link LoveRecord} 可见性、情侣成员校验一致。
 */
public interface LoveRecordSocialService {

    /** 为列表中的记录批量附加点赞数、评论数、当前用户是否已赞。 */
    List<LoveRecordResponse> enrichWithSocial(String userId, List<LoveRecord> rows);

    /** 单条记录附加互动统计（详情、新建返回等）。 */
    LoveRecordResponse enrichSingle(String userId, LoveRecord row);

    /** 切换点赞状态。 */
    LoveRecordLikeStateResponse toggleLike(String userId, String recordId);

    /** 分页拉取评论，按时间正序。 */
    LoveRecordCommentPageResponse pageComments(String userId, String recordId, long page, long pageSize);

    /** 发表评论。 */
    LoveRecordCommentResponse addComment(String userId, String recordId, LoveRecordCommentCreateRequest req);

    /** 删除自己的评论。 */
    void deleteComment(String userId, String recordId, long commentId);
}
