package com.ourgalaxy.space;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ourgalaxy.common.exception.BusinessException;
import com.ourgalaxy.space.dto.CreateSpaceRequest;
import com.ourgalaxy.space.dto.InvitationVO;
import com.ourgalaxy.space.dto.JoinSpaceRequest;
import com.ourgalaxy.space.dto.MemberVO;
import com.ourgalaxy.space.dto.SpaceVO;
import com.ourgalaxy.user.User;
import com.ourgalaxy.user.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SpaceService {

    private static final String OWNER = "OWNER";
    private static final String PARTNER = "PARTNER";
    private static final String PENDING = "PENDING";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleMemberMapper memberMapper;
    private final CoupleInvitationMapper invitationMapper;
    private final UserMapper userMapper;
    private final int invitationExpireHours;

    public SpaceService(
            CoupleSpaceMapper spaceMapper,
            CoupleMemberMapper memberMapper,
            CoupleInvitationMapper invitationMapper,
            UserMapper userMapper,
            @Value("${app.couple.invitation-expire-hours}") int invitationExpireHours
    ) {
        this.spaceMapper = spaceMapper;
        this.memberMapper = memberMapper;
        this.invitationMapper = invitationMapper;
        this.userMapper = userMapper;
        this.invitationExpireHours = invitationExpireHours;
    }

    @Transactional
    public SpaceVO createSpace(Long userId, CreateSpaceRequest request) {
        requireActiveUser(userId);
        if (findMembership(userId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "你已经加入一个情侣空间");
        }

        CoupleSpace space = new CoupleSpace();
        space.setName(request.name().trim());
        space.setAnniversaryDate(request.anniversaryDate());
        space.setDescription(trimToNull(request.description()));
        space.setCreatedBy(userId);
        spaceMapper.insert(space);

        CoupleMember owner = new CoupleMember();
        owner.setCoupleId(space.getId());
        owner.setUserId(userId);
        owner.setRole(OWNER);
        owner.setJoinedAt(LocalDateTime.now(ZoneOffset.UTC));
        memberMapper.insert(owner);

        return toVO(space, owner);
    }

    public SpaceVO currentSpace(Long userId) {
        CoupleMember membership = findMembership(userId);
        if (membership == null) {
            return null;
        }
        CoupleSpace space = spaceMapper.selectById(membership.getCoupleId());
        if (space == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "情侣空间不存在");
        }
        return toVO(space, membership);
    }

    @Transactional
    public InvitationVO createInvitation(Long userId) {
        CoupleMember currentMembership = findMembership(userId);
        CoupleMember membership = requireMember(userId, currentMembership == null
                ? null : currentMembership.getCoupleId());
        if (!OWNER.equals(membership.getRole())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有空间所有者可以生成邀请码");
        }

        CoupleSpace space = spaceMapper.selectByIdForUpdate(membership.getCoupleId());
        if (space == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "情侣空间不存在");
        }
        if (countMembers(space.getId()) >= 2) {
            throw new BusinessException(HttpStatus.CONFLICT, "空间已满，无法继续邀请");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<CoupleInvitation> pending = invitationMapper.selectList(Wrappers.<CoupleInvitation>lambdaQuery()
                .eq(CoupleInvitation::getCoupleId, space.getId())
                .eq(CoupleInvitation::getStatus, PENDING)
                .gt(CoupleInvitation::getExpiresAt, now)
                .orderByDesc(CoupleInvitation::getCreateTime));
        if (!pending.isEmpty()) {
            return toInvitationVO(pending.get(0));
        }

        CoupleInvitation invitation = new CoupleInvitation();
        invitation.setCoupleId(space.getId());
        invitation.setInviterId(userId);
        invitation.setCode(generateCode());
        invitation.setStatus(PENDING);
        invitation.setExpiresAt(now.plusHours(invitationExpireHours));
        invitationMapper.insert(invitation);
        return toInvitationVO(invitation);
    }

    @Transactional
    public SpaceVO joinSpace(Long userId, JoinSpaceRequest request) {
        requireActiveUser(userId);
        if (findMembership(userId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "你已经加入一个情侣空间");
        }

        String code = request.code().trim().toUpperCase();
        CoupleInvitation invitation = invitationMapper.selectByCodeForUpdate(code);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (invitation == null || !PENDING.equals(invitation.getStatus())
                || !invitation.getExpiresAt().isAfter(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邀请码无效或已过期");
        }

        CoupleSpace space = spaceMapper.selectByIdForUpdate(invitation.getCoupleId());
        if (space == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "情侣空间不存在");
        }
        if (countMembers(space.getId()) >= 2) {
            throw new BusinessException(HttpStatus.CONFLICT, "该空间已满");
        }

        CoupleMember partner = new CoupleMember();
        partner.setCoupleId(space.getId());
        partner.setUserId(userId);
        partner.setRole(PARTNER);
        partner.setJoinedAt(now);
        memberMapper.insert(partner);

        invitation.setStatus("USED");
        invitation.setUsedById(userId);
        invitation.setUsedAt(now);
        invitationMapper.updateById(invitation);
        return toVO(space, partner);
    }

    public CoupleMember requireMember(Long userId, Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "请先创建或加入情侣空间");
        }
        CoupleMember member = memberMapper.selectOne(Wrappers.<CoupleMember>lambdaQuery()
                .eq(CoupleMember::getCoupleId, coupleId)
                .eq(CoupleMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "资源不存在或无权访问");
        }
        return member;
    }

    public Long currentCoupleId(Long userId) {
        CoupleMember member = findMembership(userId);
        return member == null ? null : member.getCoupleId();
    }

    private void requireActiveUser(Long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
    }

    private CoupleMember findMembership(Long userId) {
        return memberMapper.selectOne(Wrappers.<CoupleMember>lambdaQuery()
                .eq(CoupleMember::getUserId, userId)
                .orderByAsc(CoupleMember::getJoinedAt, CoupleMember::getId)
                .last("LIMIT 1"));
    }

    private long countMembers(Long coupleId) {
        return memberMapper.selectCount(Wrappers.<CoupleMember>lambdaQuery()
                .eq(CoupleMember::getCoupleId, coupleId));
    }

    private SpaceVO toVO(CoupleSpace space, CoupleMember currentMember) {
        List<CoupleMember> members = memberMapper.selectList(Wrappers.<CoupleMember>lambdaQuery()
                .eq(CoupleMember::getCoupleId, space.getId())
                .orderByAsc(CoupleMember::getJoinedAt, CoupleMember::getId));
        List<Long> userIds = members.stream().map(CoupleMember::getUserId).toList();
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<MemberVO> memberVOs = members.stream()
                .map(member -> new MemberVO(
                        member.getUserId(),
                        users.get(member.getUserId()) == null ? "未知成员" : users.get(member.getUserId()).getNickname(),
                        member.getRole(),
                        member.getJoinedAt()
                ))
                .toList();
        return new SpaceVO(
                space.getId(),
                space.getName(),
                space.getAnniversaryDate(),
                space.getDescription(),
                currentMember.getRole(),
                memberVOs.size(),
                memberVOs
        );
    }

    private InvitationVO toInvitationVO(CoupleInvitation invitation) {
        return new InvitationVO(invitation.getCode(), invitation.getStatus(), invitation.getExpiresAt());
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
