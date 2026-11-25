package com.ourgalaxy.space;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("couple_invitation")
public class CoupleInvitation extends BaseEntity {

    private Long coupleId;

    private Long inviterId;

    private String code;

    private String status;

    private LocalDateTime expiresAt;

    private Long usedById;

    private LocalDateTime usedAt;
}
