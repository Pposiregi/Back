package com.fitpet.server.shared.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/redis")
public class RedisTestController {

    private final StringRedisTemplate redisTemplate;

    // 1. 데이터 저장 테스트
    @PostMapping
    public String saveData(@RequestParam String key, @RequestParam String value) {
        redisTemplate.opsForValue().set(key, value);
        return "저장 완료: " + key + " = " + value;
    }

    // 2. 데이터 조회 테스트
    @GetMapping
    public String getData(@RequestParam String key) {
        String value = redisTemplate.opsForValue().get(key);
        return "조회 결과: " + (value != null ? value : "데이터 없음");
    }
}