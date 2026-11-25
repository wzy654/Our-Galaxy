package com.ourgalaxy.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourgalaxy.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("file_object")
public class FileObject extends BaseEntity {

    private Long coupleId;

    private Long uploaderId;

    private String storageProvider;

    private String bucket;

    private String storageKey;

    private String originalName;

    private String mimeType;

    private Long sizeBytes;

    private String checksumSha256;

    private Integer width;

    private Integer height;

    private Integer durationSeconds;
}
