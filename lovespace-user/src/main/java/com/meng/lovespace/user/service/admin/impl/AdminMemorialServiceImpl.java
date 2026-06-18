package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.entity.MemorialDay;
import com.meng.lovespace.user.mapper.MemorialDayMapper;
import com.meng.lovespace.user.service.admin.AdminMemorialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminMemorialServiceImpl implements AdminMemorialService {

    private final MemorialDayMapper memorialDayMapper;

    public AdminMemorialServiceImpl(MemorialDayMapper memorialDayMapper) {
        this.memorialDayMapper = memorialDayMapper;
    }

    @Override
    public IPage<MemorialDay> page(String coupleId, long page, long pageSize) {
        LambdaQueryWrapper<MemorialDay> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(coupleId)) {
            qw.eq(MemorialDay::getCoupleId, coupleId.trim());
        }
        qw.orderByDesc(MemorialDay::getUpdatedAt);
        return memorialDayMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    public void delete(String adminUserId, String id) {
        if (!memorialDayMapper.exists(new LambdaQueryWrapper<MemorialDay>().eq(MemorialDay::getId, id))) {
            throw new ApiBusinessException(40400, "memorial day not found");
        }
        memorialDayMapper.deleteById(id);
        log.info("admin.memorial.delete adminUserId={} memorialDayId={}", adminUserId, id);
    }
}
