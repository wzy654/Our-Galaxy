# Our Galaxy 数据库 ER 图

## ER 图

```mermaid
erDiagram
    user ||--o{ couple_member : joins
    couple_space ||--o{ couple_member : contains
    user ||--o{ couple_space : creates
    couple_space ||--o{ couple_invitation : owns
    user ||--o{ couple_invitation : invites

    couple_space ||--o{ memory_event : owns
    user ||--o{ memory_event : creates
    memory_event ||--o{ memory_media : contains
    memory_event ||--o{ memory_tag : has
    couple_space ||--o{ file_object : owns
    user ||--o{ file_object : uploads
    file_object ||--o{ memory_media : stores

    couple_space ||--o{ capsule : owns
    user ||--o{ capsule : writes
    user ||--o{ capsule : receives
    user ||--o{ capsule : opens

    couple_space ||--o{ ai_record : owns
    user ||--o{ ai_record : creates

    couple_space ||--o{ place_memory : owns
    user ||--o{ place_memory : creates
    place_memory ||--o{ place_media : contains
    file_object ||--o{ place_media : stores

    couple_space ||--o{ annual_report : owns
    user ||--o{ annual_report : generates

    user {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar nickname
        bigint avatar_file_id FK
        tinyint status
        datetime create_time
        datetime update_time
        tinyint deleted
    }

    couple_space {
        bigint id PK
        varchar name
        date anniversary_date
        bigint created_by FK
        datetime create_time
        datetime update_time
        tinyint deleted
    }

    couple_member {
        bigint id PK
        bigint couple_id FK
        bigint user_id FK
        enum role
        datetime joined_at
        tinyint deleted
    }

    couple_invitation {
        bigint id PK
        bigint couple_id FK
        bigint inviter_id FK
        varchar code UK
        enum status
        datetime expires_at
        bigint used_by_id FK
        datetime used_at
        tinyint deleted
    }

    memory_event {
        bigint id PK
        bigint couple_id FK
        bigint creator_id FK
        varchar title
        text content
        date event_date
        decimal latitude
        decimal longitude
        datetime create_time
        datetime update_time
        tinyint deleted
    }

    memory_media {
        bigint id PK
        bigint couple_id FK
        bigint memory_id FK
        bigint file_id FK
        enum media_type
        int sort_order
        tinyint deleted
    }

    memory_tag {
        bigint id PK
        bigint couple_id FK
        bigint memory_id FK
        varchar name
        tinyint deleted
    }

    file_object {
        bigint id PK
        bigint couple_id FK
        bigint uploader_id FK
        varchar purpose
        varchar storage_key UK
        varchar mime_type
        bigint size_bytes
        char checksum_sha256
        tinyint deleted
    }

    capsule {
        bigint id PK
        bigint couple_id FK
        bigint creator_id FK
        bigint recipient_user_id FK
        varchar title
        mediumtext content
        datetime open_at
        datetime opened_at
        enum status
        tinyint deleted
    }

    ai_record {
        bigint id PK
        bigint couple_id FK
        bigint user_id FK
        enum feature
        text prompt
        mediumtext response
        enum status
        datetime create_time
        datetime update_time
        tinyint deleted
    }

    place_memory {
        bigint id PK
        bigint couple_id FK
        bigint creator_id FK
        varchar name
        enum category
        decimal latitude
        decimal longitude
        tinyint deleted
    }

    place_media {
        bigint id PK
        bigint couple_id FK
        bigint place_id FK
        bigint file_id FK
        int sort_order
        tinyint deleted
    }

    annual_report {
        bigint id PK
        bigint couple_id FK
        bigint generated_by_id FK
        smallint report_year UK
        json statistics
        enum status
        varchar pdf_storage_key
        tinyint deleted
    }
```

## 关键约束

1. 空间业务表都包含 `couple_id`，详情、修改、删除和回忆文件下载必须同时校验资源 ID 与当前空间 ID；个人头像文件是例外，`file_object.couple_id` 允许为 `NULL`，必须按 `uploader_id` 隔离。
2. `couple_member` 通过 `(couple_id, user_id)` 唯一索引防止重复加入；每个空间最多两名正式成员的限制由 Phase 1 的空间行锁与成员事务共同保证。
3. `memory_media`、`memory_tag`、`place_media` 使用 `(资源 ID, couple_id)` 复合外键，避免把其他空间的资源挂接到当前空间。
4. `file_object.storage_key` 全局唯一；`purpose=MEMORY` 时校验空间成员关系与 `couple_id`，`purpose=AVATAR` 时仅允许 `uploader_id` 本人访问，两类文件不能互换用途。
5. `capsule.content` 在数据库中持久化，但未到 `open_at` 或状态不是 `OPENED` 时，服务端不得返回正文。
6. `annual_report.statistics` 保存真实统计快照；AI 只生成叙事文本，不得生成或修改统计数字。
7. `user.email`、`couple_invitation.code`、`file_object.storage_key` 和 `(couple_id, report_year)` 使用唯一索引。
8. 所有主键为 `BIGINT`，字符集为 `utf8mb4`，业务表支持软删除。

## 时间规范

- `DATETIME(6)` 表示 UTC 时间，数据库连接统一设置 `serverTimezone=UTC`，Java 层使用 `Instant` 或 `OffsetDateTime`。
- `DATE` 只表示纪念日、回忆日期等本地日历日期，不附加时区。
- 创建时间使用数据库当前 UTC 时间，更新时间由 `ON UPDATE CURRENT_TIMESTAMP(6)` 维护。
- Redis Key、对象存储路径和导出文件名都应包含 `coupleId`；个人头像路径包含 `userId`。路径本身不能作为权限依据。
