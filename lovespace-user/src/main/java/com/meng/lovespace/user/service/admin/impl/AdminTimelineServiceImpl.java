package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.entity.LoveRecord;
import com.meng.lovespace.user.mapper.LoveRecordCommentMapper;
import com.meng.lovespace.user.mapper.LoveRecordLikeMapper;
import com.meng.lovespace.user.mapper.LoveRecordMapper;
import com.meng.lovespace.user.service.admin.AdminTimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminTimelineServiceImpl implements AdminTimelineService {

    private final LoveRecordMapper loveRecordMapper;
    private final LoveRecordCommentMapper commentMapper;
    private final LoveRecordLikeMapper likeMapper;

    public AdminTimelineServiceImpl(
            LoveRecordMapper loveRecordMapper,
            LoveRecordCommentMapper commentMapper,
            LoveRecordLikeMapper likeMapper) {
        this.loveRecordMapper = loveRecordMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
    }

    @Override
    public IPage<LoveRecord> page(String coupleId, String userId, long page, long pageSize) {
        LambdaQueryWrapper<LoveRecord> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(coupleId)) {
            qw.eq(LoveRecord::getCoupleId, coupleId.trim());
        }
        if (StringUtils.hasText(userId)) {
            qw.eq(LoveRecord::getAuthorId, userId.trim());
        }
        qw.orderByDesc(LoveRecord::getRecordDate).orderByDesc(LoveRecord::getCreatedAt);
        return loveRecordMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String adminUserId, String recordId) {
        LoveRecord record = loveRecordMapper.selectById(recordId);
        if (record == null) {
            throw new ApiBusinessException(40400, "record not found");
        }
        commentMapper.delete(
                new LambdaQueryWrapper<com.meng.lovespace.user.entity.LoveRecordComment>()
                        .eq(com.meng.lovespace.user.entity.LoveRecordComment::getRecordId, recordId));
        likeMapper.delete(
                new LambdaQueryWrapper<com.meng.lovespace.user.entity.LoveRecordLike>()
                        .eq(com.meng.lovespace.user.entity.LoveRecordLike::getRecordId, recordId));
        loveRecordMapper.deleteById(recordId);
        log.info("admin.timeline.delete adminUserId={} recordId={}", adminUserId, recordId);
    }
}
