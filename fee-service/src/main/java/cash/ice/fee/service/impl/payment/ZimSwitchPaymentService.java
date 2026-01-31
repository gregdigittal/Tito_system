package cash.ice.fee.service.impl.payment;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ZimSwitchPaymentService implements PaymentService {

    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        log.info(">>> ZimSwitch process payment for vendorRef: {}", feesData.getVendorRef());
        // todo
    }

    @Override
    public void processRefund(ErrorData errorData) {
        log.info(">>>>>> ZimSwitch process refund for vendorRef: {}", errorData.getFeesData().getVendorRef());
        // todo
    }
}
