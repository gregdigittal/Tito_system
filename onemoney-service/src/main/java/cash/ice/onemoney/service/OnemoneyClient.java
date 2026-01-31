package cash.ice.onemoney.service;

import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.api_requestmgr.Response;

public interface OnemoneyClient {

    Response sendPayment(Request request, String paymentUrl);

    com.huawei.cps.synccpsinterface.api_requestmgr.Result sendStatusRequest(com.huawei.cps.synccpsinterface.api_requestmgr.Request request, String statusUrl);

    Response sendRefundRequest(Request request, String reversalUrl);
}
