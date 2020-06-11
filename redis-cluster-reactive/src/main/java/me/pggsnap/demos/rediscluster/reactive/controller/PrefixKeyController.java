package me.pggsnap.demos.rediscluster.reactive.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author pggsnap
 * @date 2020/6/9
 */
@RestController
@RequestMapping("/redis-reactive")
public class PrefixKeyController {

    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @GetMapping("/keys")
    public Mono<List<String>> testKeys(@RequestParam("prefix") String prefix) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Flux<String> keys = reactiveRedisTemplate.keys(prefix + "*");
        return keys
                .collectList()
                .doOnSuccess(list -> {
                    stopWatch.stop();
                    System.out.println("prefix key: " + prefix + ", count: " + list.size() + ", cost: " + stopWatch.getLastTaskTimeMillis());
                });
    }

    @GetMapping("/scan")
    public Mono<HashSet<String>> testScan(@RequestParam("prefix") String prefix) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        return reactiveRedisTemplate.execute(connection ->
                connection.keyCommands().scan(ScanOptions.scanOptions().count(1000).match(prefix + "*").build()))
                .map(byteBuffer -> new String(byteBuffer.array(), StandardCharsets.UTF_8))
                .collectList()
                .map(list -> new HashSet<>(list))
                .doOnSuccess(ret -> {
                    stopWatch.stop();
                    System.out.println("prefix key: " + prefix + ", count: " + ret.size() + ", cost: " + stopWatch.getLastTaskTimeMillis());
                });
    }

    @GetMapping("/hscan")
    public Mono<HashMap<String, Object>> testHscan(@RequestParam("prefix") String prefix) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ReactiveHashOperations<String, String, Object> opsForHash = reactiveRedisTemplate.opsForHash();
        return opsForHash.scan(prefix, ScanOptions.scanOptions().count(10).match("a*").build())
                .reduce(new HashMap<String, Object>(), (map, entry) -> {
                    map.put(entry.getKey(), entry.getValue());
                    return map;
                })
                .doOnSuccess(ret -> {
                    stopWatch.stop();
                    System.out.println("prefix key: " + prefix + ", count: " + ret.size() + ", cost: " + stopWatch.getLastTaskTimeMillis());
                });
    }


    @GetMapping("/get")
    public Mono<Object> testGet(@RequestParam("key") String key) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Mono value = reactiveRedisTemplate.opsForValue().get(key);
        return value
                .doOnSuccess(v -> {
                    stopWatch.stop();
                    System.out.println("key: " + key + ", value: " + v + ", cost: " + stopWatch.getLastTaskTimeMillis());
                });
    }
}
