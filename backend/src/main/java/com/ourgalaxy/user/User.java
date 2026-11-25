package com.ourgalaxy.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user")
public class User extends BaseEntity {

    private String email;

    private String passwordHash;

    private String nickname;

    private Long avatarFileId;

    private Integer status;
}
