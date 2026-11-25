ALTER TABLE `file_object`
    MODIFY COLUMN `couple_id` BIGINT NULL,
    ADD COLUMN `purpose` VARCHAR(16) NOT NULL DEFAULT 'MEMORY' AFTER `uploader_id`,
    ADD KEY `idx_file_object_owner_purpose` (`uploader_id`, `purpose`, `deleted`),
    ADD CONSTRAINT `ck_file_object_purpose` CHECK (`purpose` IN ('MEMORY', 'AVATAR'));
