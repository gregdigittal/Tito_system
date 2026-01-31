package cash.ice.api.service.security;

import cash.ice.sqldb.entity.IpRange;
import cash.ice.sqldb.repository.IpRangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpWhitelistCacheableDataService {
    private final IpRangeRepository ipRangeRepository;

    @Cacheable(value = "whitelistIpRange")
    public List<IpRange> getActiveIpRanges() {
        return ipRangeRepository.findByActive(true);
    }
}