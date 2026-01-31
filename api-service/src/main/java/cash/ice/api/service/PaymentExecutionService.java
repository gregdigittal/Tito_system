package cash.ice.api.service;

import cash.ice.api.entity.zim.Payment;
import cash.ice.sqldb.entity.PaymentLine;

import java.util.List;

public interface PaymentExecutionService {

    void execute(Payment payment, List<PaymentLine> paymentLines);
}
