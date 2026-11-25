package com.ourgalaxy.memory;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ourgalaxy.common.exception.BusinessException;
import com.ourgalaxy.file.FileObject;
import com.ourgalaxy.file.FileMapper;
import com.ourgalaxy.file.FileService;
import com.ourgalaxy.file.dto.FileVO;
import com.ourgalaxy.memory.dto.MemoryMediaVO;
import com.ourgalaxy.memory.dto.MemoryRequest;
import com.ourgalaxy.memory.dto.MemoryVO;
import com.ourgalaxy.space.SpaceService;
import com.ourgalaxy.user.User;
import com.ourgalaxy.user.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    private final MemoryEventMapper memoryMapper;
    private final MemoryMediaMapper mediaMapper;
    private final MemoryTagMapper tagMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;
    private final SpaceService spaceService;
    private final UserMapper userMapper;

    public MemoryService(
            MemoryEventMapper memoryMapper,
            MemoryMediaMapper mediaMapper,
            MemoryTagMapper tagMapper,
            FileMapper fileMapper,
            FileService fileService,
            SpaceService spaceService,
            UserMapper userMapper
    ) {
        this.memoryMapper = memoryMapper;
        this.mediaMapper = mediaMapper;
        this.tagMapper = tagMapper;
        this.fileMapper = fileMapper;
        this.fileService = fileService;
        this.spaceService = spaceService;
        this.userMapper = userMapper;
    }

    @Transactional
    public MemoryVO create(Long userId, MemoryRequest request) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);

        MemoryEvent memory = new MemoryEvent();
        memory.setCoupleId(coupleId);
        memory.setCreatorId(userId);
        applyRequest(memory, request);
        memoryMapper.insert(memory);
        reconcileAssociations(memory, request);
        return get(userId, memory.getId());
    }

    public List<MemoryVO> list(Long userId) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);
        List<MemoryEvent> memories = memoryMapper.selectList(Wrappers.<MemoryEvent>lambdaQuery()
                .eq(MemoryEvent::getCoupleId, coupleId)
                .orderByDesc(MemoryEvent::getEventDate, MemoryEvent::getId));
        return toVOs(memories);
    }

    public MemoryVO get(Long userId, Long memoryId) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);
        MemoryEvent memory = findMemory(coupleId, memoryId);
        return toVO(memory);
    }

    @Transactional
    public MemoryVO update(Long userId, Long memoryId, MemoryRequest request) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);
        MemoryEvent memory = findMemory(coupleId, memoryId);
        applyRequest(memory, request);
        memory.setUpdateTime(LocalDateTime.now(ZoneOffset.UTC));
        memoryMapper.updateById(memory);
        reconcileAssociations(memory, request);
        return get(userId, memoryId);
    }

    @Transactional
    public void delete(Long userId, Long memoryId) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);
        findMemory(coupleId, memoryId);
        mediaMapper.delete(Wrappers.<MemoryMedia>lambdaQuery()
                .eq(MemoryMedia::getCoupleId, coupleId)
                .eq(MemoryMedia::getMemoryId, memoryId));
        tagMapper.delete(Wrappers.<MemoryTag>lambdaQuery()
                .eq(MemoryTag::getCoupleId, coupleId)
                .eq(MemoryTag::getMemoryId, memoryId));
        memoryMapper.delete(Wrappers.<MemoryEvent>lambdaQuery()
                .eq(MemoryEvent::getCoupleId, coupleId)
                .eq(MemoryEvent::getId, memoryId));
    }

    private MemoryEvent findMemory(Long coupleId, Long memoryId) {
        MemoryEvent memory = memoryMapper.selectOne(Wrappers.<MemoryEvent>lambdaQuery()
                .eq(MemoryEvent::getId, memoryId)
                .eq(MemoryEvent::getCoupleId, coupleId));
        if (memory == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "回忆不存在或无权访问");
        }
        return memory;
    }

    private void applyRequest(MemoryEvent memory, MemoryRequest request) {
        memory.setTitle(request.title().trim());
        memory.setContent(trimToNull(request.content()));
        memory.setEventDate(request.eventDate());
        memory.setLocationName(trimToNull(request.locationName()));
        memory.setLatitude(request.latitude());
        memory.setLongitude(request.longitude());
        memory.setStarType(request.starType() == null || request.starType().isBlank()
                ? "CLASSIC" : request.starType().trim());
    }

    private void reconcileAssociations(MemoryEvent memory, MemoryRequest request) {
        List<Long> requestedFileIds = request.mediaFileIds() == null ? List.of() : request.mediaFileIds();
        if (requestedFileIds.contains(null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "图片 ID 不能为空");
        }
        List<Long> fileIds = requestedFileIds.stream().distinct().toList();
        List<MemoryMedia> mediaHistory = mediaMapper.selectHistory(memory.getCoupleId(), memory.getId());

        for (MemoryMedia existing : mediaHistory) {
            if (existing.getDeleted() == 0 && !fileIds.contains(existing.getFileId())) {
                mediaMapper.deleteById(existing.getId());
            }
        }
        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);
            FileObject file = fileService.requireFile(memory.getCoupleId(), fileId);
            MemoryMedia active = mediaHistory.stream()
                    .filter(item -> item.getDeleted() == 0 && fileId.equals(item.getFileId()))
                    .findFirst()
                    .orElse(null);
            if (active != null) {
                active.setSortOrder(i);
                mediaMapper.updateById(active);
                continue;
            }

            MemoryMedia deleted = mediaHistory.stream()
                    .filter(item -> item.getDeleted() != 0 && fileId.equals(item.getFileId()))
                    .findFirst()
                    .orElse(null);
            if (deleted != null) {
                mediaMapper.restore(deleted.getId(), memory.getCoupleId(), memory.getId(), i);
                continue;
            }

            MemoryMedia media = new MemoryMedia();
            media.setCoupleId(memory.getCoupleId());
            media.setMemoryId(memory.getId());
            media.setFileId(file.getId());
            media.setMediaType(file.getMimeType().startsWith("image/") ? "IMAGE" : "VIDEO");
            media.setSortOrder(i);
            mediaMapper.insert(media);
        }

        Map<String, String> uniqueTags = new LinkedHashMap<>();
        if (request.tags() != null) {
            for (String tag : request.tags()) {
                String normalized = tag.trim();
                uniqueTags.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
            }
        }
        List<MemoryTag> tagHistory = tagMapper.selectHistory(memory.getCoupleId(), memory.getId());
        Map<String, MemoryTag> activeTags = tagHistory.stream()
                .filter(item -> item.getDeleted() == 0)
                .collect(Collectors.toMap(
                        item -> item.getName().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, second) -> first
                ));

        for (MemoryTag existing : activeTags.values()) {
            if (!uniqueTags.containsKey(existing.getName().toLowerCase(Locale.ROOT))) {
                tagMapper.deleteById(existing.getId());
            }
        }
        for (Map.Entry<String, String> entry : uniqueTags.entrySet()) {
            MemoryTag active = activeTags.get(entry.getKey());
            if (active != null) {
                if (!active.getName().equals(entry.getValue())) {
                    active.setName(entry.getValue());
                    tagMapper.updateById(active);
                }
                continue;
            }

            MemoryTag deleted = tagHistory.stream()
                    .filter(item -> item.getDeleted() != 0
                            && entry.getKey().equals(item.getName().toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .orElse(null);
            if (deleted != null) {
                tagMapper.restore(deleted.getId(), memory.getCoupleId(), memory.getId(), entry.getValue());
                continue;
            }

            MemoryTag memoryTag = new MemoryTag();
            memoryTag.setCoupleId(memory.getCoupleId());
            memoryTag.setMemoryId(memory.getId());
            memoryTag.setName(entry.getValue());
            tagMapper.insert(memoryTag);
        }
    }

    private List<MemoryVO> toVOs(List<MemoryEvent> memories) {
        if (memories.isEmpty()) {
            return List.of();
        }
        List<Long> memoryIds = memories.stream().map(MemoryEvent::getId).toList();
        List<Long> creatorIds = memories.stream().map(MemoryEvent::getCreatorId).distinct().toList();
        Map<Long, User> creators = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, List<String>> tags = tagMapper.selectList(Wrappers.<MemoryTag>lambdaQuery()
                        .eq(MemoryTag::getCoupleId, memories.get(0).getCoupleId())
                        .in(MemoryTag::getMemoryId, memoryIds)
                        .orderByAsc(MemoryTag::getId))
                .stream()
                .collect(Collectors.groupingBy(MemoryTag::getMemoryId,
                        Collectors.mapping(MemoryTag::getName, Collectors.toList())));

        List<MemoryMedia> media = mediaMapper.selectList(Wrappers.<MemoryMedia>lambdaQuery()
                .eq(MemoryMedia::getCoupleId, memories.get(0).getCoupleId())
                .in(MemoryMedia::getMemoryId, memoryIds)
                .orderByAsc(MemoryMedia::getSortOrder, MemoryMedia::getId));
        Map<Long, List<MemoryMediaVO>> mediaGrouped = buildMedia(media);

        return memories.stream()
                .map(memory -> toVO(memory,
                        creators.get(memory.getCreatorId()),
                        tags.getOrDefault(memory.getId(), List.of()),
                        mediaGrouped.getOrDefault(memory.getId(), List.of())))
                .toList();
    }

    private MemoryVO toVO(MemoryEvent memory) {
        List<MemoryTag> tags = tagMapper.selectList(Wrappers.<MemoryTag>lambdaQuery()
                .eq(MemoryTag::getCoupleId, memory.getCoupleId())
                .eq(MemoryTag::getMemoryId, memory.getId())
                .orderByAsc(MemoryTag::getId));
        List<MemoryMedia> media = mediaMapper.selectList(Wrappers.<MemoryMedia>lambdaQuery()
                .eq(MemoryMedia::getCoupleId, memory.getCoupleId())
                .eq(MemoryMedia::getMemoryId, memory.getId())
                .orderByAsc(MemoryMedia::getSortOrder, MemoryMedia::getId));
        User creator = userMapper.selectById(memory.getCreatorId());
        return toVO(
                memory,
                creator,
                tags.stream().map(MemoryTag::getName).toList(),
                buildMedia(media).getOrDefault(memory.getId(), List.of())
        );
    }

    private Map<Long, List<MemoryMediaVO>> buildMedia(List<MemoryMedia> media) {
        if (media.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> fileIds = media.stream().map(MemoryMedia::getFileId).distinct().toList();
        Map<Long, FileObject> files = fileMapper.selectBatchIds(fileIds).stream()
                .collect(Collectors.toMap(FileObject::getId, Function.identity()));
        Map<Long, List<MemoryMediaVO>> result = new LinkedHashMap<>();
        for (MemoryMedia item : media) {
            FileObject file = files.get(item.getFileId());
            if (file != null) {
                result.computeIfAbsent(item.getMemoryId(), key -> new ArrayList<>()).add(new MemoryMediaVO(
                        item.getId(), item.getFileId(), item.getSortOrder(), fileService.toVO(file)
                ));
            }
        }
        return result;
    }

    private MemoryVO toVO(
            MemoryEvent memory,
            User creator,
            List<String> tags,
            List<MemoryMediaVO> media
    ) {
        return new MemoryVO(
                memory.getId(),
                memory.getTitle(),
                memory.getContent(),
                memory.getEventDate(),
                memory.getLocationName(),
                memory.getLatitude(),
                memory.getLongitude(),
                memory.getStarType(),
                memory.getCreatorId(),
                creator == null ? "未知成员" : creator.getNickname(),
                memory.getCreateTime(),
                memory.getUpdateTime(),
                tags,
                media
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
