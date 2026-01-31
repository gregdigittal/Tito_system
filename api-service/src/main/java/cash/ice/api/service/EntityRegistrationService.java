package cash.ice.api.service;

import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.sqldb.entity.*;

import java.util.Map;

public interface EntityRegistrationService {

    RegisterResponse registerEntity(RegisterEntityRequest request);

    RegisterResponse registerEntity(RegisterEntityRequest request, String pin, String internalId, String pvv, String accountNumber, String currencyCode, boolean sendPinBySms);

    RegisterResponse registerEntity(RegisterEntityRequest request, String pin, String internalId, String pvv, String accountNumber, Currency currency, boolean sendPinBySms);

    boolean isExistsId(Integer idTypeId, String idNumber, boolean forceCheck);

    boolean isExistsEmail(String email, boolean forceCheck);

    boolean isExistsMsisdn(String msisdn, boolean checkOnlyPrimary, boolean forceCheck);

    EntityClass saveEntity(RegisterEntityRequest request, String internalId, String pvv, Map<String, Object> addToMetadata);

    EntityMsisdn saveMsisdn(EntityClass entityClass, MsisdnType type, String msisdn, String contactName);

    Address saveAddress(EntityClass entity, AddressType addressType, RegisterEntityRequest.Address address);

    Account saveAccount(EntityClass entity, Currency currency, String accountTypeName, AuthorisationType authorisationType);

    String generateInternalId();

    String generateAccountNumber();
}
