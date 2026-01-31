package cash.ice.fee.service;

import cash.ice.fee.dto.group.GroupApiPaymentRequest;

import java.io.IOException;

public interface GroupApiPaymentQueryService {

    String query(GroupApiPaymentRequest paymentRequest) throws IOException;
}
