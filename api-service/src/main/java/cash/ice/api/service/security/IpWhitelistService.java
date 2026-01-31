package cash.ice.api.service.security;

import cash.ice.api.util.HttpUtils;
import cash.ice.sqldb.entity.IpRange;
import cash.ice.sqldb.repository.IpRangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("IpWhitelist")
@RequiredArgsConstructor
@Slf4j
public class IpWhitelistService {
    private final IpWhitelistCacheableDataService ipWhitelistCacheableDataService;
    private final IpRangeRepository ipRangeRepository;

    @Value("${ice.cash.ip-whitelist.disable-check:false}")
    private boolean disableIpWhitelistCheck;

    @Value("${ice.cash.ip-whitelist.password:null}")
    private String password;

    public boolean check() {
        if (!disableIpWhitelistCheck) {
            String ipAddress = HttpUtils.getRequestIP();
            for (IpRange ipRange : ipWhitelistCacheableDataService.getActiveIpRanges()) {
                IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(ipRange.getAddress());
                if (ipAddressMatcher.matches(ipAddress)) {
                    return true;
                }
            }
            log.warn("{} is not in a whitelist, rejecting", ipAddress);
            return false;
        }
        return true;
    }

    public IpRange addIp(String ip, String description, String password) {
        if (Objects.equals(this.password, password)) {
            return ipRangeRepository.save(new IpRange().setAddress(ip).setDescription(description).setActive(true));
        } else {
            throw new IllegalArgumentException("Wrong password");
        }
    }

    public IpRange updateIp(String ip, String description, Boolean active, String password) {
        if (Objects.equals(this.password, password)) {
            IpRange ipRange = ipRangeRepository.findByAddress(ip).orElseThrow();
            if (description != null) {
                ipRange.setDescription(description);
            }
            if (active != null) {
                ipRange.setActive(active);
            }
            return ipRangeRepository.save(ipRange);
        } else {
            throw new IllegalArgumentException("Wrong password");
        }
    }

    public IpRange removeIp(String ip, String password) {
        if (Objects.equals(this.password, password)) {
            IpRange ipRange = ipRangeRepository.findByAddress(ip).orElseThrow();
            ipRangeRepository.delete(ipRange);
            return ipRange;
        } else {
            throw new IllegalArgumentException("Wrong password");
        }
    }
}