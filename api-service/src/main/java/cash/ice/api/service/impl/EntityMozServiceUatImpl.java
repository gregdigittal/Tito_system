package cash.ice.api.service.impl;

import cash.ice.api.config.property.DeploymentConfigProperties;
import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.Me60MozService;
import cash.ice.api.service.OtpService;
import cash.ice.api.service.PermissionsService;
import cash.ice.api.service.TopUpServiceSelector;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class EntityMozServiceUatImpl extends EntityMozServiceImpl {
    public EntityMozServiceUatImpl(EntityRepository entityRepository, PermissionsService permissionsService, OtpService otpService, Me60MozService me60MozService, AccountRepository accountRepository, AccountTypeRepository accountTypeRepository, CurrencyRepository currencyRepository, DeviceRepository deviceRepository, InitiatorRepository initiatorRepository, InitiatorStatusRepository initiatorStatusRepository, AccountBalanceRepository accountBalanceRepository, EntityMsisdnRepository entityMsisdnRepository, MozProperties mozProperties, DeploymentConfigProperties deploymentConfigProperties, TopUpServiceSelector topUpServiceSelector) {
        super(entityRepository, permissionsService, otpService, me60MozService, accountRepository, accountTypeRepository, currencyRepository, deviceRepository, initiatorRepository, initiatorStatusRepository, accountBalanceRepository, entityMsisdnRepository, mozProperties, deploymentConfigProperties, topUpServiceSelector);
    }

    @Override
    public EntityClass getAuthEntity(AuthUser authUser, ConfigInput config) {
        if (config != null && config.getAuth() != null) {
            return entityRepository.findById(config.getAuth()).orElseThrow(() ->
                    new UnexistingUserException(String.valueOf(config.getAuth())));
        } else {
            return super.getAuthEntity(authUser, config);
        }
    }
}
