package com.meng.lovespace.user.service.admin.impl;

import com.meng.lovespace.user.dto.admin.AdminDashboardStatsResponse;
import com.meng.lovespace.user.mapper.AlbumMapper;
import com.meng.lovespace.user.mapper.CoupleBindingMapper;
import com.meng.lovespace.user.mapper.LoveQaConversationMapper;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import com.meng.lovespace.user.mapper.LoveRecordMapper;
import com.meng.lovespace.user.mapper.MemorialDayMapper;
import com.meng.lovespace.user.mapper.MessageMapper;
import com.meng.lovespace.user.mapper.PhotoMapper;
import com.meng.lovespace.user.mapper.PlanMapper;
import com.meng.lovespace.user.mapper.UserMapper;
import com.meng.lovespace.user.service.admin.AdminDashboardService;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final CoupleBindingMapper coupleBindingMapper;
    private final LoveRecordMapper loveRecordMapper;
    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;
    private final MessageMapper messageMapper;
    private final PlanMapper planMapper;
    private final MemorialDayMapper memorialDayMapper;
    private final LoveQaDocumentMapper loveQaDocumentMapper;
    private final LoveQaConversationMapper loveQaConversationMapper;

    public AdminDashboardServiceImpl(
            UserMapper userMapper,
            CoupleBindingMapper coupleBindingMapper,
            LoveRecordMapper loveRecordMapper,
            AlbumMapper albumMapper,
            PhotoMapper photoMapper,
            MessageMapper messageMapper,
            PlanMapper planMapper,
            MemorialDayMapper memorialDayMapper,
            LoveQaDocumentMapper loveQaDocumentMapper,
            LoveQaConversationMapper loveQaConversationMapper) {
        this.userMapper = userMapper;
        this.coupleBindingMapper = coupleBindingMapper;
        this.loveRecordMapper = loveRecordMapper;
        this.albumMapper = albumMapper;
        this.photoMapper = photoMapper;
        this.messageMapper = messageMapper;
        this.planMapper = planMapper;
        this.memorialDayMapper = memorialDayMapper;
        this.loveQaDocumentMapper = loveQaDocumentMapper;
        this.loveQaConversationMapper = loveQaConversationMapper;
    }

    @Override
    public AdminDashboardStatsResponse stats() {
        return new AdminDashboardStatsResponse(
                userMapper.selectCount(null),
                coupleBindingMapper.selectCount(null),
                loveRecordMapper.selectCount(null),
                albumMapper.selectCount(null),
                photoMapper.selectCount(null),
                messageMapper.selectCount(null),
                planMapper.selectCount(null),
                memorialDayMapper.selectCount(null),
                loveQaDocumentMapper.selectCount(null),
                loveQaConversationMapper.selectCount(null));
    }
}
