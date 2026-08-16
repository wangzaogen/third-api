package com.thirdapi.admin.web;

import com.thirdapi.admin.service.HealthCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/health-checks")
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Long channelId) {
        return healthCheckService.list(channelId);
    }

    @PostMapping("/run")
    public List<Map<String, Object>> run(@RequestParam Long channelId) {
        return healthCheckService.run(channelId);
    }
}
