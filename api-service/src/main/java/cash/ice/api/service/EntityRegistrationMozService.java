package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.dto.moz.*;
import cash.ice.sqldb.entity.Dictionary;

import java.util.Locale;

public interface EntityRegistrationMozService {

    RegisterResponse registerEntity(RegisterEntityMozRequest request);

    RegisterMozResponse registerUser(RegisterEntityMozRequest request, OptionalEntityRegisterData optionalData, AuthUser authUser, String otp, boolean removeDocumentsOnFail);

    RegisterMozResponse registerCorporateUser(RegisterCompanyMozRequest company, RegisterEntityMozRequest representative, OptionalEntityRegisterData optionalData, AuthUser authUser, String otp, boolean removeDocumentsOnFail);

    Dictionary getRegistrationAgreement(Locale locale, AccountTypeMoz accountType);
}
