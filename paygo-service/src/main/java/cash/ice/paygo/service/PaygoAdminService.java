package cash.ice.paygo.service;

import cash.ice.paygo.dto.admin.*;

import java.util.List;

public interface PaygoAdminService {

    List<FinancialInstitution> getFinancialInstitutions();

    List<Merchant> getMerchants();

    Merchant addMerchant(MerchantCreate request);

    Merchant updateMerchant(Merchant merchant);

    void deleteMerchant(String merchantId);

    List<Credential> getCredentials(String merchantId);

    Credential addCredential(CredentialCreate request);

    void updateCredential(Credential credential);

    void deleteCredential(String credentialId);

    Expiration getPaymentExpiration(String deviceReference);

    PaymentStatus getPaymentStatus(String deviceReference);
}
