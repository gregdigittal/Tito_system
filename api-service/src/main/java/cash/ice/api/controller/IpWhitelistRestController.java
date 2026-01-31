package cash.ice.api.controller;

import cash.ice.api.service.security.IpWhitelistService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.sqldb.entity.IpRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/whitelist")
@RequiredArgsConstructor
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class IpWhitelistRestController {
    private final IpWhitelistService ipWhitelistService;

    @PostMapping("/ip")
    public IpRange addWhitelistIp(@RequestParam String ip, @RequestParam String description, @RequestParam String password) {
        log.info("> Adding IP to whitelist: {}, description: {}", ip, description);
        return ipWhitelistService.addIp(ip, description, password);
    }

    @PutMapping("/ip")
    public IpRange updateWhitelistIp(@RequestParam String ip, @RequestParam(required = false) Boolean active, @RequestParam(required = false) String description, @RequestParam String password) {
        log.info("> Updating whitelist IP: {}, active: {}, description: {}", ip, active, description);
        return ipWhitelistService.updateIp(ip, description, active, password);
    }

    @DeleteMapping("/ip")
    public IpRange deleteWhitelistIp(@RequestParam String ip, @RequestParam String password) {
        log.info("> Deleting whitelist IP: {}", ip);
        return ipWhitelistService.removeIp(ip, password);
    }
}
