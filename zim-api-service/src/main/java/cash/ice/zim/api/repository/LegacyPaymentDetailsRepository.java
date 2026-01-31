package cash.ice.zim.api.repository;

import cash.ice.zim.api.entity.LegacyPaymentDetails;

import java.util.Optional;

public interface LegacyPaymentDetailsRepository extends ReadOnlyRepository<LegacyPaymentDetails, Integer> {

    Optional<LegacyPaymentDetails> findByPaymentId(Integer paymentId);
}
