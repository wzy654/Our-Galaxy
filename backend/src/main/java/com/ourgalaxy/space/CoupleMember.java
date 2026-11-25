package com.ourgalaxy.space;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("couple_member")
public class CoupleMember extends BaseEntity {

    private Long coupleId;

    private Long userId;

    private String role;

    private LocalDateTime joinedAt;
}
