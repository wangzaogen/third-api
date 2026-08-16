package com.thirdapi.admin.web;

import com.thirdapi.admin.dto.ConfigSnapshotDto;
import com.thirdapi.admin.dto.PublishResponse;
import com.thirdapi.admin.service.ConfigPublishService;
import com.thirdapi.admin.service.ConfigQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/apps/{appId}")
public class ConfigController {

    private final ConfigQueryService queryService;
    private final ConfigPublishService publishService;

    public ConfigController(ConfigQueryService queryService, ConfigPublishService publishService) {
        this.queryService = queryService;
        this.publishService = publishService;
    }

    @GetMapping("/configs")
    public ResponseEntity<ConfigSnapshotDto> pull(@PathVariable String appId,
                                                  @RequestParam(defaultValue = "0") long version,
                                                  @RequestParam(defaultValue = "30") int longPoll) throws InterruptedException {
        queryService.requireAppId(appId);
        long current = queryService.currentVersion(appId);
        if (current <= version) {
            long waitMs = Math.min(Math.max(longPoll, 0), 30) * 1000L;
            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }
            current = queryService.currentVersion(appId);
            if (current <= version) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        }
        return ResponseEntity.ok(queryService.loadCurrentSnapshot(appId));
    }

    @PostMapping("/publish")
    public PublishResponse publish(@PathVariable String appId,
                                   @RequestParam(defaultValue = "admin") String operator) {
        ConfigSnapshotDto snapshot = publishService.publish(appId, operator);
        return new PublishResponse(appId, snapshot.getVersion(), true);
    }
}
