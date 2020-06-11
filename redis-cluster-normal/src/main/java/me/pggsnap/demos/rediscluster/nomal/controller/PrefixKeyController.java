package me.pggsnap.demos.rediscluster.nomal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * @author pggsnap
 * @date 2020/6/9
 */
@RestController
@RequestMapping("/redis-normal")
public class PrefixKeyController {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/keys")
    public Set<String> testKeys(@RequestParam("prefix") String prefix) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Set<String> keys = redisTemplate.keys(prefix + "*");
        stopWatch.stop();
        System.out.println("prefix key: " + prefix + ", count: " + keys.size() + ", cost: " + stopWatch.getLastTaskTimeMillis());
        return keys;
    }

    @GetMapping("/scan")
    public Set<String> testScan(@RequestParam("prefix") String prefix) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Set<String> result = new HashSet<>();

        redisTemplate.execute((RedisCallback<String>) connection -> {
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().count(1000).match(prefix + "*").build());
            cursor.forEachRemaining(item -> result.add(new String(item, StandardCharsets.UTF_8)));
            return null;
        });

        stopWatch.stop();
        System.out.println("prefix key: " + prefix + ", count: " + result.size() + ", cost: " + stopWatch.getLastTaskTimeMillis());
        return result;
    }

    @GetMapping("/get")
    public Object testGet(@RequestParam("key") String key) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object value = redisTemplate.opsForValue().get(key);
        stopWatch.stop();
        System.out.println("key: " + key + ", value: " + value + ", cost: " + stopWatch.getLastTaskTimeMillis());
        return value;
    }
}
