package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.api.dto.moz.LookupEntityType;
import cash.ice.api.dto.moz.MoneyProviderMoz;
import cash.ice.api.dto.moz.TagInfoMoz;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.Initiator;
import cash.ice.sqldb.entity.MsisdnType;
import cash.ice.sqldb.entity.moz.Device;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface EntityMozService {

    EntityClass getAuthEntity(AuthUser authUser, ConfigInput config);

    EntityClass getEntityById(Integer id);

    Page<Initiator> getEntityInitiators(EntityClass entity, int page, int size, SortInput sort);

    Page<Device> getEntityDevices(EntityClass entity, boolean linkedToVehicle, int page, int size, SortInput sort);

    TagInfoMoz getTagInfo(String tagNumber);

    Account getAccount(EntityClass entity, String accountType, String currencyCode);

    List<EntityClass> lookupEntity(LookupEntityType lookupBy, IdTypeMoz idType, String value);

    EntityClass addOrUpdateMsisdn(EntityClass entity, MsisdnType type, String mobile, String oldMobile, String description, String otp);

    PaymentResponse topupAccount(EntityClass authEntity, String accountNumber, MoneyProviderMoz provider, String mobile, BigDecimal amount);

    /** Phase 8-2: Cash-out from Tito wallet to mobile money. Not yet wired to provider; throws until implemented. */
    PaymentResponse cashOutToMobileMoney(EntityClass authEntity, String accountNumber, MoneyProviderMoz provider, String mobile, BigDecimal amount);

    /**
     * Phase 8-6: Internal cash-out by account id (e.g. for sweep). No auth-entity check; for system use only.
     * When provider is implemented, shared with user-facing cashOutToMobileMoney.
     */
    PaymentResponse cashOutToMobileMoneyByAccountId(Integer accountId, MoneyProviderMoz provider, String mobile, BigDecimal amount);
}
