package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.user.dto.LoveRecordCommentCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordCommentPageResponse;
import com.meng.lovespace.user.dto.LoveRecordCommentResponse;
import com.meng.lovespace.user.dto.LoveRecordLikeStateResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.entity.LoveRecord;
import com.meng.lovespace.user.entity.LoveRecordComment;
import com.meng.lovespace.user.entity.LoveRecordLike;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import com.meng.lovespace.user.mapper.LoveRecordCommentMapper;
import com.meng.lovespace.user.mapper.LoveRecordLikeMapper;
import com.meng.lovespace.user.mapper.LoveRecordMapper;
import com.meng.lovespace.user.mapper.UserMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.LoveRecordSocialService;
import com.meng.lovespace.user.timeline.LoveRecordVisibility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LoveRecordSocialService} 实现：批量统计、点赞切换、评论分页与增删。
 */
@Slf4j
@Service
public class LoveRecordSocialServiceImpl implements LoveRecordSocialService {

    private final LoveRecordMapper loveRecordMapper;
    private final LoveRecordLikeMapper likeMapper;
    private final LoveRecordCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final CoupleBindingService coupleBindingService;

    public LoveRecordSocialServiceImpl(
            LoveRecordMapper loveRecordMapper,
            LoveRecordLikeMapper likeMapper,
            LoveRecordCommentMapper commentMapper,
            UserMapper userMapper,
            CoupleBindingService coupleBindingService) {
        this.loveRecordMapper = loveRecordMapper;
        this.likeMapper = likeMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.coupleBindingService = coupleBindingService;
    }

    @Override
    public List<LoveRecordResponse> enrichWithSocial(String userId, List<LoveRecord> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> ids = rows.stream().map(LoveRecord::getId).toList();
        Map<String, Long> likeCounts = countLikesByRecordIds(ids);
        Map<String, Long> commentCounts = countCommentsByRecordIds(ids);
        Set<String> likedIds = likedRecordIdsForUser(userId, ids);
        return rows.stream()
                .map(
                        r ->
                                buildResponse(
                                        r,
                                        likeCounts.getOrDefault(r.getId(), 0L).intValue(),
                                        commentCounts.getOrDefault(r.getId(), 0L).intValue(),
                                        likedIds.contains(r.getId())))
                .toList();
    }

    @Override
    public LoveRecordResponse enrichSingle(String userId, LoveRecord row) {
        List<LoveRecordResponse> one = enrichWithSocial(userId, List.of(row));
        return one.getFirst();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoveRecordLikeStateResponse toggleLike(String userId, String recordId) {
        LoveRecord record = requireViewableRecord(userId, recordId);
        LambdaQueryWrapper<LoveRecordLike> w =
                new LambdaQueryWrapper<LoveRecordLike>()
                        .eq(LoveRecordLike::getRecordId, recordId)
                        .eq(LoveRecordLike::getUserId, userId);
        LoveRecordLike existing = likeMapper.selectOne(w);
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            log.info("loveRecord.likeRemoved recordId={} userId={}", recordId, userId);
        } else {
            LoveRecordLike row = new LoveRecordLike();
            row.setRecordId(recordId);
            row.setUserId(userId);
            likeMapper.insert(row);
            log.info("loveRecord.likeAdded recordId={} userId={}", recordId, userId);
        }
        int total = countLikes(recordId);
        boolean liked = existing == null;
        return new LoveRecordLikeStateResponse(total, liked);
    }

    @Override
    public LoveRecordCommentPageResponse pageComments(String userId, String recordId, long page, long pageSize) {
        requireViewableRecord(userId, recordId);
        long p = Math.max(1, page);
        long ps = Math.max(1, Math.min(pageSize, 50));
        Page<LoveRecordComment> pg = new Page<>(p, ps);
        LambdaQueryWrapper<LoveRecordComment> w =
                new LambdaQueryWrapper<LoveRecordComment>()
                        .eq(LoveRecordComment::getRecordId, recordId)
                        .orderByAsc(LoveRecordComment::getCreatedAt);
        Page<LoveRecordComment> result = commentMapper.selectPage(pg, w);
        List<LoveRecordComment> recs = result.getRecords();
        Map<String, String> usernames = loadUsernames(recs.stream().map(LoveRecordComment::getUserId).distinct().toList());
        List<LoveRecordCommentResponse> list =
                recs.stream()
                        .map(
                                c ->
                                        new LoveRecordCommentResponse(
                                                c.getId(),
                                                c.getRecordId(),
                                                c.getUserId(),
                                                usernames.getOrDefault(c.getUserId(), ""),
                                                c.getContent(),
                                                c.getCreatedAt()))
                        .toList();
        return new LoveRecordCommentPageResponse(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoveRecordCommentResponse addComment(String userId, String recordId, LoveRecordCommentCreateRequest req) {
        requireViewableRecord(userId, recordId);
        String text = req.content().trim();
        if (text.isEmpty()) {
            throw new TimelineBusinessException(40056, "content is empty");
        }
        if (text.length() > 500) {
            throw new TimelineBusinessException(40057, "content too long");
        }
        LoveRecordComment row = new LoveRecordComment();
        row.setRecordId(recordId);
        row.setUserId(userId);
        row.setContent(text);
        commentMapper.insert(row);
        User u = userMapper.selectById(userId);
        String name = u != null && u.getUsername() != null ? u.getUsername() : "";
        log.info("loveRecord.commentAdded recordId={} userId={} commentId={}", recordId, userId, row.getId());
        return new LoveRecordCommentResponse(
                row.getId(), recordId, userId, name, row.getContent(), row.getCreatedAt());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(String userId, String recordId, long commentId) {
        requireViewableRecord(userId, recordId);
        LoveRecordComment c = commentMapper.selectById(commentId);
        if (c == null || !recordId.equals(c.getRecordId())) {
            throw new TimelineBusinessException(40455, "comment not found");
        }
        if (!userId.equals(c.getUserId())) {
            throw new TimelineBusinessException(40355, "only author can delete comment");
        }
        commentMapper.deleteById(commentId);
        log.info("loveRecord.commentDeleted recordId={} userId={} commentId={}", recordId, userId, commentId);
    }

    private LoveRecord requireViewableRecord(String userId, String recordId) {
        LoveRecord record = loveRecordMapper.selectById(recordId);
        if (record == null) {
            throw new TimelineBusinessException(40452, "record not found");
        }
        assertCoupleMember(userId, record.getCoupleId());
        if (!canView(record, userId)) {
            throw new TimelineBusinessException(40352, "forbidden to view this record");
        }
        return record;
    }

    private void assertCoupleMember(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new TimelineBusinessException(40351, "forbidden or invalid couple"));
    }

    private static boolean canView(LoveRecord row, String viewerId) {
        if (row.getVisibility() == null) {
            return false;
        }
        if (Objects.equals(row.getVisibility(), LoveRecordVisibility.COUPLE)) {
            return true;
        }
        return Objects.equals(row.getVisibility(), LoveRecordVisibility.SELF)
                && Objects.equals(row.getAuthorId(), viewerId);
    }

    private Map<String, Long> countLikesByRecordIds(List<String> ids) {
        LambdaQueryWrapper<LoveRecordLike> w = new LambdaQueryWrapper<LoveRecordLike>().in(LoveRecordLike::getRecordId, ids);
        List<LoveRecordLike> list = likeMapper.selectList(w);
        return list.stream().collect(Collectors.groupingBy(LoveRecordLike::getRecordId, Collectors.counting()));
    }

    private Map<String, Long> countCommentsByRecordIds(List<String> ids) {
        LambdaQueryWrapper<LoveRecordComment> w =
                new LambdaQueryWrapper<LoveRecordComment>().in(LoveRecordComment::getRecordId, ids);
        List<LoveRecordComment> list = commentMapper.selectList(w);
        return list.stream().collect(Collectors.groupingBy(LoveRecordComment::getRecordId, Collectors.counting()));
    }

    private Set<String> likedRecordIdsForUser(String userId, List<String> recordIds) {
        LambdaQueryWrapper<LoveRecordLike> w =
                new LambdaQueryWrapper<LoveRecordLike>()
                        .eq(LoveRecordLike::getUserId, userId)
                        .in(LoveRecordLike::getRecordId, recordIds);
        return likeMapper.selectList(w).stream().map(LoveRecordLike::getRecordId).collect(Collectors.toSet());
    }

    private int countLikes(String recordId) {
        LambdaQueryWrapper<LoveRecordLike> w =
                new LambdaQueryWrapper<LoveRecordLike>().eq(LoveRecordLike::getRecordId, recordId);
        return Math.toIntExact(likeMapper.selectCount(w));
    }

    private Map<String, String> loadUsernames(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<String, String> map = new HashMap<>();
        for (User u : users) {
            if (u.getUsername() != null) {
                map.put(u.getId(), u.getUsername());
            }
        }
        return map;
    }

    private static LoveRecordResponse buildResponse(LoveRecord r, int likeCount, int commentCount, boolean likedByMe) {
        return new LoveRecordResponse(
                r.getId(),
                r.getCoupleId(),
                r.getAuthorId(),
                r.getRecordDate(),
                r.getContent(),
                r.getMood(),
                r.getLocationJson(),
                r.getVisibility(),
                r.getTagsJson(),
                r.getImagesJson(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                likeCount,
                commentCount,
                likedByMe);
    }
}
