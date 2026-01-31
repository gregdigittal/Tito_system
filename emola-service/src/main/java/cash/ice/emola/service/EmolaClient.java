package cash.ice.emola.service;

import com.viettel.bccsgw.webservice.GwOperation;
import com.viettel.bccsgw.webservice.GwOperationResponse;

public interface EmolaClient {

    GwOperationResponse sendPayment(GwOperation request, String paymentUrl);
}
