package cash.ice.zim.api.repository;

import cash.ice.zim.api.entity.LegacyPayments;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LegacyPaymentsRepository extends ReadOnlyRepository<LegacyPayments, Integer> {

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE Transactions_Card SET Status_ID = ?2, Cancelled_Date = GETDATE(), Cancelled_By = ?3 WHERE Transactions_Card_ID = ?1")
    void updateTransactionCard(int transactionCardId, Integer statusId, Integer accountId);

    @Query(nativeQuery = true, value = "SELECT TOP 1 Transactions_Card_ID FROM Transactions_Card WHERE Vendor_Reference = ?1 ORDER BY Transactions_Card_ID DESC")
    Integer getTransactionCardIdBy(String vendorRef);

    @Query(nativeQuery = true, value = "SELECT TOP 1 Status FROM Payments WHERE Payment_ID = ?1 ORDER BY Payment_ID DESC")
    Integer getPaymentStatus(Integer paymentId);            // 0 - pending, 1 - approved, 2 - declined, 4 - pending bank processing
}
