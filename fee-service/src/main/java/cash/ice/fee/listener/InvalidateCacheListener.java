package cash.ice.fee.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidateCacheListener {
    private final CacheManager cacheManager;

    @KafkaListener(groupId = "${ice.cash.fee.group}", topics = {"${ice.cash.kafka.topic.invalidate-cache}"})
    void listenToInvalidateCacheTopic(ConsumerRecord<String, List<String>> rec) {
        log.info(">> invalidate cache: " + rec.value());
        rec.value().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                log.debug("  invalidating cache: " + cacheName);
                cache.invalidate();
            }
        });
    }
}
