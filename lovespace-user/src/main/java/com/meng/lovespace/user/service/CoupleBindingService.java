package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meng.lovespace.user.dto.CoupleAcceptRequest;
import com.meng.lovespace.user.dto.CoupleInfoResponse;
import com.meng.lovespace.user.dto.CoupleInviteResponse;
import com.meng.lovespace.user.dto.CouplePendingInviteResponse;
import com.meng.lovespace.user.entity.CoupleBinding;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 情侣绑定：邀请、接受、查询、更新开始日、解除关系。
 */
public interface CoupleBindingService extends IService<CoupleBinding> {

    /**
     * 当前用户向对方发起绑定邀请（按对方手机号查找用户）。
     *
     * @param inviterId 邀请方用户 ID
     * @param inviteePhone 被邀请方手机号
     * @return 邀请记录 ID
     */
    CoupleInviteResponse invite(String inviterId, String inviteePhone);

    /**
     * 被邀请方接受邀请，状态变为交往中，并规范化双方 ID 顺序。
     */
    CoupleInfoResponse accept(String currentUserId, CoupleAcceptRequest request);

    /**
     * 被邀请方：待处理邀请列表（状态为待接受、且当前用户为被邀请方 {@code user_id2}）。
     */
    List<CouplePendingInviteResponse> listPendingInvitesForInvitee(String inviteeUserId);

    /** 被邀请方：待处理邀请条数。 */
    long countPendingInvitesForInvitee(String inviteeUserId);

    /**
     * 查询当前用户进行中的情侣信息（交往或冻结）。
     */
    Optional<CoupleInfoResponse> getCoupleInfo(String userId);

    /**
     * 更新当前情侣的恋爱开始日并回写恋爱天数。
     */
    void updateStartDate(String userId, LocalDate startDate);

    /** 将当前情侣关系标记为已解除。 */
    void separate(String userId);

    /**
     * 若 {@code coupleId} 存在且状态为交往/冻结，且 {@code userId} 为成员之一，则返回该绑定。
     *
     * @param userId 当前用户
     * @param coupleId 情侣绑定主键（即 couple_binding.id）
     */
    Optional<CoupleBinding> findActiveOrFrozenMembership(String userId, String coupleId);
}
