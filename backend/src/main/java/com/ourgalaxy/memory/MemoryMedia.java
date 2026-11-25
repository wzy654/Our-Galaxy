package com.ourgalaxy.memory;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("memory_media")
public class MemoryMedia extends BaseEntity {

    private Long coupleId;

    private Long memoryId;

    private Long fileId;

    private String mediaType;

    private Integer sortOrder;
}
