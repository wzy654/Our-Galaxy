package com.ourgalaxy.space;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("couple_space")
public class CoupleSpace extends BaseEntity {

    private String name;

    private LocalDate anniversaryDate;

    private String description;

    private Long createdBy;
}
