CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(64) NOT NULL,
    `avatar_file_id` BIGINT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_deleted` (`deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `couple_space` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `anniversary_date` DATE NULL,
    `description` VARCHAR(500) NULL,
    `created_by` BIGINT NOT NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_couple_space_creator` (`created_by`),
    KEY `idx_couple_space_deleted` (`deleted`),
    CONSTRAINT `fk_couple_space_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `couple_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` ENUM('OWNER', 'PARTNER') NOT NULL,
    `joined_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple_member_couple_user` (`couple_id`, `user_id`),
    KEY `idx_couple_member_user` (`user_id`),
    CONSTRAINT `fk_couple_member_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_couple_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `couple_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `inviter_id` BIGINT NOT NULL,
    `code` VARCHAR(32) NOT NULL,
    `status` ENUM('PENDING', 'USED', 'EXPIRED', 'REVOKED') NOT NULL DEFAULT 'PENDING',
    `expires_at` DATETIME(6) NOT NULL,
    `used_by_id` BIGINT NULL,
    `used_at` DATETIME(6) NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple_invitation_code` (`code`),
    KEY `idx_couple_invitation_space_status` (`couple_id`, `status`),
    KEY `idx_couple_invitation_expires` (`expires_at`),
    KEY `idx_couple_invitation_inviter` (`inviter_id`),
    KEY `idx_couple_invitation_used_by` (`used_by_id`),
    CONSTRAINT `fk_couple_invitation_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_couple_invitation_inviter` FOREIGN KEY (`inviter_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_couple_invitation_used_by` FOREIGN KEY (`used_by_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `memory_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `creator_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NULL,
    `event_date` DATE NOT NULL,
    `location_name` VARCHAR(255) NULL,
    `latitude` DECIMAL(10, 7) NULL,
    `longitude` DECIMAL(10, 7) NULL,
    `star_type` VARCHAR(32) NOT NULL DEFAULT 'CLASSIC',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_event_id_couple` (`id`, `couple_id`),
    KEY `idx_memory_event_space_date` (`couple_id`, `event_date`),
    KEY `idx_memory_event_space_creator` (`couple_id`, `creator_id`),
    KEY `idx_memory_event_space_deleted` (`couple_id`, `deleted`),
    CONSTRAINT `fk_memory_event_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_memory_event_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_memory_event_latitude` CHECK (`latitude` IS NULL OR `latitude` BETWEEN -90 AND 90),
    CONSTRAINT `ck_memory_event_longitude` CHECK (`longitude` IS NULL OR `longitude` BETWEEN -180 AND 180)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `file_object` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `uploader_id` BIGINT NOT NULL,
    `storage_provider` VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    `bucket` VARCHAR(255) NULL,
    `storage_key` VARCHAR(512) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(150) NOT NULL,
    `size_bytes` BIGINT NOT NULL,
    `checksum_sha256` CHAR(64) NULL,
    `width` INT NULL,
    `height` INT NULL,
    `duration_seconds` INT NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_object_id_couple` (`id`, `couple_id`),
    UNIQUE KEY `uk_file_object_storage_key` (`storage_key`),
    KEY `idx_file_object_space_uploader` (`couple_id`, `uploader_id`),
    KEY `idx_file_object_space_deleted` (`couple_id`, `deleted`),
    CONSTRAINT `fk_file_object_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_file_object_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_file_object_size` CHECK (`size_bytes` > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE `user`
    ADD CONSTRAINT `fk_user_avatar_file` FOREIGN KEY (`avatar_file_id`) REFERENCES `file_object` (`id`);

CREATE TABLE `memory_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `memory_id` BIGINT NOT NULL,
    `file_id` BIGINT NOT NULL,
    `media_type` ENUM('IMAGE', 'VIDEO') NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_media_memory_file` (`memory_id`, `file_id`),
    KEY `idx_memory_media_space_memory` (`couple_id`, `memory_id`),
    CONSTRAINT `fk_memory_media_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_memory_media_memory` FOREIGN KEY (`memory_id`, `couple_id`) REFERENCES `memory_event` (`id`, `couple_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_memory_media_file` FOREIGN KEY (`file_id`, `couple_id`) REFERENCES `file_object` (`id`, `couple_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `memory_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `memory_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_tag_memory_name` (`memory_id`, `name`),
    KEY `idx_memory_tag_space_memory` (`couple_id`, `memory_id`),
    CONSTRAINT `fk_memory_tag_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_memory_tag_memory` FOREIGN KEY (`memory_id`, `couple_id`) REFERENCES `memory_event` (`id`, `couple_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `capsule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `creator_id` BIGINT NOT NULL,
    `recipient_user_id` BIGINT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` MEDIUMTEXT NOT NULL,
    `open_at` DATETIME(6) NOT NULL,
    `opened_at` DATETIME(6) NULL,
    `opened_by_id` BIGINT NULL,
    `status` ENUM('LOCKED', 'OPENED') NOT NULL DEFAULT 'LOCKED',
    `remind_at` DATETIME(6) NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_capsule_space_status_open` (`couple_id`, `status`, `open_at`),
    KEY `idx_capsule_space_recipient` (`couple_id`, `recipient_user_id`),
    KEY `idx_capsule_creator` (`creator_id`),
    KEY `idx_capsule_opened_by` (`opened_by_id`),
    CONSTRAINT `fk_capsule_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_capsule_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_capsule_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_capsule_opened_by` FOREIGN KEY (`opened_by_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `ai_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `feature` ENUM('STORY', 'ANNIVERSARY', 'COMMUNICATION', 'ANNUAL_REPORT', 'FUTURE_SIMULATOR') NOT NULL,
    `prompt` TEXT NOT NULL,
    `response` MEDIUMTEXT NULL,
    `provider` VARCHAR(64) NULL,
    `model` VARCHAR(128) NULL,
    `status` ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    `error_message` VARCHAR(1000) NULL,
    `token_usage` INT NULL,
    `latency_ms` BIGINT NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ai_record_space_user_time` (`couple_id`, `user_id`, `create_time`),
    KEY `idx_ai_record_space_feature` (`couple_id`, `feature`),
    KEY `idx_ai_record_space_deleted` (`couple_id`, `deleted`),
    CONSTRAINT `fk_ai_record_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_ai_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `place_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `creator_id` BIGINT NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `category` ENUM('FIRST_DATE', 'FIRST_TRIP', 'ANNIVERSARY', 'DAILY', 'FUTURE_PLAN') NOT NULL,
    `address` VARCHAR(500) NULL,
    `latitude` DECIMAL(10, 7) NOT NULL,
    `longitude` DECIMAL(10, 7) NOT NULL,
    `visited_at` DATETIME(6) NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_place_memory_id_couple` (`id`, `couple_id`),
    KEY `idx_place_memory_space_category` (`couple_id`, `category`),
    KEY `idx_place_memory_space_creator` (`couple_id`, `creator_id`),
    KEY `idx_place_memory_space_deleted` (`couple_id`, `deleted`),
    CONSTRAINT `fk_place_memory_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_place_memory_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_place_memory_latitude` CHECK (`latitude` BETWEEN -90 AND 90),
    CONSTRAINT `ck_place_memory_longitude` CHECK (`longitude` BETWEEN -180 AND 180)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `place_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `place_id` BIGINT NOT NULL,
    `file_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_place_media_place_file` (`place_id`, `file_id`),
    KEY `idx_place_media_space_place` (`couple_id`, `place_id`),
    CONSTRAINT `fk_place_media_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_place_media_place` FOREIGN KEY (`place_id`, `couple_id`) REFERENCES `place_memory` (`id`, `couple_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_place_media_file` FOREIGN KEY (`file_id`, `couple_id`) REFERENCES `file_object` (`id`, `couple_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `annual_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL,
    `generated_by_id` BIGINT NOT NULL,
    `report_year` SMALLINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `summary` TEXT NULL,
    `keywords` JSON NULL,
    `statistics` JSON NOT NULL,
    `ai_story` MEDIUMTEXT NULL,
    `wishes` TEXT NULL,
    `pdf_storage_key` VARCHAR(512) NULL,
    `status` ENUM('GENERATING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'GENERATING',
    `generated_at` DATETIME(6) NULL,
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_annual_report_space_year` (`couple_id`, `report_year`),
    KEY `idx_annual_report_space_status` (`couple_id`, `status`),
    KEY `idx_annual_report_generator` (`generated_by_id`),
    CONSTRAINT `fk_annual_report_space` FOREIGN KEY (`couple_id`) REFERENCES `couple_space` (`id`),
    CONSTRAINT `fk_annual_report_generator` FOREIGN KEY (`generated_by_id`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_annual_report_year` CHECK (`report_year` BETWEEN 2000 AND 2100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
