package com.ourgalaxy.memory;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@TableName("memory_event")
public class MemoryEvent extends BaseEntity {

    private Long coupleId;

    private Long creatorId;

    private String title;

    private String content;

    private LocalDate eventDate;

    private String locationName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String starType;
}
