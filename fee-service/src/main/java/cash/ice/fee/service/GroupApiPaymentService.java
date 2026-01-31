package cash.ice.fee.service;

import cash.ice.fee.dto.PublicApiPayment;
import cash.ice.fee.dto.group.GroupApiPaymentResponse;

import java.io.IOException;

public interface GroupApiPaymentService {

    GroupApiPaymentResponse callGroupApiPayment(PublicApiPayment apiPayment) throws IOException;
}
