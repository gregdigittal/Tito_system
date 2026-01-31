package cash.ice.api.service;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;

import java.util.List;

public interface TicketService {

    void createTicketFor(List<PaymentResponse> failedPayments, List<PaymentRequest> paymentRequestList);
}
