package com.ourgalaxy.memory;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("memory_tag")
public class MemoryTag extends BaseEntity {

    private Long coupleId;

    private Long memoryId;

    private String name;
}
