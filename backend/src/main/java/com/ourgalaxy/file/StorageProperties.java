package com.ourgalaxy.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file")
public record StorageProperties(String localRoot, long maxSizeBytes) {
}
