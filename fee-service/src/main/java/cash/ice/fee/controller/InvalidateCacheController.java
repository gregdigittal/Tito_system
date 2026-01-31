package cash.ice.fee.controller;

import cash.ice.common.service.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/cache"})
@RequiredArgsConstructor
@Slf4j
public class InvalidateCacheController {
    private final KafkaSender kafkaSender;

    @PostMapping("/clear")
    public void clearCache(@RequestBody List<String> cacheNames) {
        log.info("> received clear cache: " + cacheNames);
        kafkaSender.sendInvalidateCache(cacheNames);
    }
}
