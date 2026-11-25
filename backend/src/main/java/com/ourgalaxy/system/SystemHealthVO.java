package com.ourgalaxy.system;

import java.time.Instant;

public record SystemHealthVO(String status, String service, String version, String scope, Instant timestamp) {
}
