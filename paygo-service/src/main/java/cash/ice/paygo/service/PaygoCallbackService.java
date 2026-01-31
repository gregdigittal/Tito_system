package cash.ice.paygo.service;

import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;

public interface PaygoCallbackService {

    PaygoCallbackResponse handleRequest(PaygoCallbackRequest request);
}
