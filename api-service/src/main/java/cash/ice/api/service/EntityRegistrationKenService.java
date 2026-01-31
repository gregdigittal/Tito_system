package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.moz.RegisterEntityKenRequest;
import cash.ice.api.dto.moz.RegisterKenResponse;

public interface EntityRegistrationKenService {

    RegisterKenResponse registerUser(RegisterEntityKenRequest request, AuthUser authUser, String otp, boolean removeDocumentsOnFail);
}
