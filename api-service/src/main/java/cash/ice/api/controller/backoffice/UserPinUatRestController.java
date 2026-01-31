package cash.ice.api.controller.backoffice;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.LoginData;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.repository.LoginDataRepository;
import cash.ice.api.repository.backoffice.StaffMemberRepository;
import cash.ice.api.service.EntityLoginService;
import cash.ice.api.service.SecurityPvvService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.Initiator;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.InitiatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/pin")
@RequiredArgsConstructor
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class UserPinUatRestController {
    private final SecurityPvvService securityPvvService;
    private final EntityLoginService entityLoginService;
    private final AccountRepository accountRepository;
    private final EntityRepository entityRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final InitiatorRepository initiatorRepository;
    private final LoginDataRepository loginDataRepository;
    private final EntitiesProperties entitiesProperties;
    private final StaffProperties staffProperties;

    @GetMapping
    public List<String> getUserPin(@RequestParam String accountNumber) {
        log.info("GET pin for accountNumber: " + accountNumber);
        List<Account> accounts = accountRepository.findByAccountNumber(accountNumber);
        return accounts.stream().map(account -> {
            EntityClass entity = entityRepository.findById(account.getEntityId())
                    .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", ErrorCodes.EC1048));
            return securityPvvService.restorePin(entity.getInternalId(), entity.getPvv(), entitiesProperties.getPasswordDigitsAmount());
        }).toList();
    }

    @GetMapping("/entity")
    public String getEntityPassword(@RequestParam String username) {
        log.info("GET password for entity: " + username);
        EntityClass entity = entityLoginService.findEntity(username);
        return securityPvvService.restorePin(entity.getInternalId(), entity.getPvv(), entitiesProperties.getPasswordDigitsAmount());
    }

    @GetMapping("/entity/otp")
    public String getEntityOtpPin(@RequestParam String entityId) {
        log.info("GET pin for entity OTP: " + entityId);
        LoginData loginData = loginDataRepository.findByLogin(entityId)
                .orElseThrow(() -> new UnexistingUserException(entityId));
        if (loginData.getOtpPvv() == null) {
            throw new RuntimeException("OTP PIN isn't set");
        }
        return securityPvvService.restorePin(loginData.getMfaPinKey(), loginData.getOtpPvv(), entitiesProperties.getMfa().getOtpDigitsAmount());
    }

    @GetMapping("/entity/mfa/secret")
    public String getEntitySecretCode(@RequestParam String entityId) {
        log.info("GET secretCode for entity: " + entityId);
        EntityClass entity = entityRepository.findById(Integer.valueOf(entityId)).orElseThrow();
        return entity.getMfaSecretCode();
    }

    @GetMapping("/staff")
    public String getStaffPin(@RequestParam String email) {
        log.info("GET pin for staff member: " + email);
        StaffMember staffMember = staffMemberRepository.findStaffMemberByEmail(email)
                .orElseThrow(() -> new UnexistingUserException(email));
        return securityPvvService.restorePin(staffMember.getPinKey(), staffMember.getPvv(), staffProperties.getPasswordDigitsAmount());
    }

    @GetMapping("/staff/otp")
    public String getOtpPin(@RequestParam String email) {
        log.info("GET pin for staff member OTP: " + email);
        LoginData loginData = loginDataRepository.findByLogin(email)
                .orElseThrow(() -> new UnexistingUserException(email));
        if (loginData.getOtpPvv() == null) {
            throw new RuntimeException("OTP PIN isn't set");
        }
        return securityPvvService.restorePin(loginData.getMfaPinKey(), loginData.getOtpPvv(), staffProperties.getMfa().getOtpDigitsAmount());
    }

    @GetMapping("/staff/email/key")
    public String getForgotPasswordEmailKey(@RequestParam String email) {
        log.info("GET forgot password email key: " + email);
        LoginData loginData = loginDataRepository.findByLogin(email)
                .orElseThrow(() -> new UnexistingUserException(email));
        if (loginData.getForgotPasswordKey() == null) {
            throw new RuntimeException("Forgot password email key isn't set");
        }
        return loginData.getForgotPasswordKey();
    }

    @GetMapping("/initiator")
    public String getInitiatorPin(@RequestParam String initiator) {
        log.info("GET pin for initiator: " + initiator);
        Initiator card = initiatorRepository.findByIdentifier(initiator).orElseThrow(() ->
                new ICEcashException(String.format("Initiator: %s does not exist", initiator), ErrorCodes.EC1012));
        return securityPvvService.restorePin(initiator, card.getPvv(), 4);
    }
}
