package com.ourgalaxy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    protected BaseEntity() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createTime = now;
        this.updateTime = now;
        this.deleted = 0;
    }
}
