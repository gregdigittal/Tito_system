package cash.ice.fbc.service.impl;

import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentOtpWaitingZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.dto.zim.PaymentSuccessZim;
import cash.ice.common.service.KafkaSender;
import cash.ice.fbc.config.FbcProperties;
import cash.ice.fbc.dto.*;
import cash.ice.fbc.entity.FbcPayment;
import cash.ice.fbc.repository.FbcPaymentRepository;
import cash.ice.fbc.service.FbcSenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cash.ice.fbc.service.impl.FbcPaymentServiceImpl.SERVICE_HEADER;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FbcPaymentServiceImplTest {
    private static final String VENDOR_REF = "1122334455";
    private static final String ACCOUNT_NUMBER = "123456789";
    private static final BigDecimal AMOUNT = new BigDecimal("1000");
    private static final String ZWL = "ZWL";
    private static final String PAYMENT_DESCR = "descr";
    private static final String INITIATOR_ID = "some initiator";
    private static final String PAYMENT_DETAILS = "FEES";
    private static final String BRANCH_CODE = "020";
    private static final String PHONE_NUMBER = "12341234";
    private static final String OTP = "1234";

    @Mock
    private FbcPaymentRepository fbcPaymentRepository;
    @Mock
    private FbcSenderService fbcSenderService;
    @Mock
    private KafkaSender kafkaSender;
    @Mock
    private FbcProperties fbcProperties;

    @InjectMocks
    private FbcPaymentServiceImpl fbcPaymentServiceImpl;

    @Test
    public void testProcessPayment() {
        PaymentRequestZim paymentRequest = new PaymentRequestZim().setVendorRef(VENDOR_REF).setAccountNumber(ACCOUNT_NUMBER).setAmount(AMOUNT)
                .setMetaData(Map.of(PaymentRequestZim.CURRENCY_CODE, ZWL, PaymentRequestZim.PAYMENT_DESCRIPTION, PAYMENT_DESCR));

        FbcVerificationResponse verificationResponse = new FbcVerificationResponse().setResponse(new FbcVerificationResponse.Response()
                .setCustomerAccountNumber(ACCOUNT_NUMBER).setBranchCode(BRANCH_CODE));

        when(fbcSenderService.sendAccountVerification(ACCOUNT_NUMBER)).thenReturn(verificationResponse);
        when(fbcProperties.getInitiatorId()).thenReturn(INITIATOR_ID);
        when(fbcProperties.getPaymentDetails()).thenReturn(PAYMENT_DETAILS);
        when(fbcSenderService.sendTransferSubmission(new FbcTransferSubmissionRequest().setCurrency(ZWL).setAmount(AMOUNT).setExternalReference(VENDOR_REF)
                .setInitiatorID(INITIATOR_ID).setPaymentDetails(PAYMENT_DETAILS).setSourceAccountNumber(ACCOUNT_NUMBER).setSourceBranchCode(BRANCH_CODE)))
                .thenReturn(new FbcTransferSubmissionResponse().setResponse(new FbcTransferSubmissionResponse.Response().setPhoneNumber(PHONE_NUMBER)));

        fbcPaymentServiceImpl.processPayment(paymentRequest);
        verify(fbcPaymentRepository).save(any(FbcPayment.class));
        verify(kafkaSender).sendZimPaymentResult(eq(VENDOR_REF), any(PaymentOtpWaitingZim.class));
    }

    @Test
    public void testProcessOtp() {
        PaymentOtpRequestZim paymentRequest = new PaymentOtpRequestZim().setVendorRef(VENDOR_REF).setOtp(OTP);

        when(fbcPaymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(List.of(new FbcPayment()));
        when(fbcSenderService.sendVerifyOtp(new FbcVerifyOtpRequest().setTransactionReference(VENDOR_REF).setOtp(OTP)))
                .thenReturn(new FbcVerifyOtpResponse());
        when(fbcSenderService.sendQueryStatus(VENDOR_REF)).thenReturn(new FbcStatusResponse());

        fbcPaymentServiceImpl.processOtp(paymentRequest, null);
        verify(fbcPaymentRepository).save(any(FbcPayment.class));
        verify(kafkaSender).sendZimPaymentResult(eq(VENDOR_REF), any(PaymentSuccessZim.class), any(), eq(SERVICE_HEADER));
    }
}