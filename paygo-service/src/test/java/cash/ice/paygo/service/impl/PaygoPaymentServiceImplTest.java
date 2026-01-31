package cash.ice.paygo.service.impl;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.paygo.config.PaygoProperties;
import cash.ice.paygo.dto.admin.Credential;
import cash.ice.paygo.dto.admin.Merchant;
import cash.ice.paygo.entity.PaygoMerchant;
import cash.ice.paygo.entity.PaygoPayment;
import cash.ice.paygo.repository.PaygoMerchantRepository;
import cash.ice.paygo.repository.PaygoPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaygoPaymentServiceImplTest {
    private static final String ZWL = "ZWL";
    private static final String VENDOR_REF = "test";
    private static final String PGCBZ = "PGCBZ";

    @Mock
    private RestTemplate qrRestTemplate;
    @Mock
    private PaygoPaymentRepository paygoPaymentRepository;
    @Mock
    private PaygoMerchantRepository paygoMerchantRepository;
    @Mock
    private PaygoProperties paygoProperties;
    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<PaygoPayment> paygoPaymentCaptor;

    private PaygoPaymentServiceImpl service;

    @BeforeEach
    void init() {
        service = new PaygoPaymentServiceImpl(qrRestTemplate, paygoPaymentRepository, paygoMerchantRepository,
                paygoProperties, kafkaSender);
    }

    @Test
    void processPaymentSuccessfully() {
        FeesData feesData = new FeesData().setVendorRef(VENDOR_REF).setPaymentRequest(new PaymentRequest()
                .setMeta(Map.of("description", "desc")).setTx(PGCBZ).setAmount(new BigDecimal("10"))
                .setCurrency(ZWL));
        Merchant merchant = (Merchant) new Merchant().setName("testMerchant").setAddressLine1("addr");
        Credential credential = (Credential) new Credential().setCurrencyCode(ZWL);
        String qr64 = "data:image:qrImage";

        when(paygoMerchantRepository.findByMerchantTransactionCode(PGCBZ)).thenReturn(Optional.of(
                new PaygoMerchant().setMerchant(merchant).setCredentials(List.of(credential))));
        when(paygoProperties.getIdTotalDigits()).thenReturn(7);
        when(paygoProperties.getPrefix()).thenReturn("423");
        when(qrRestTemplate.getForObject(any(), eq(String.class), any(), eq("desc"), eq("testMerchant"), eq("addr"), eq(10)))
                .thenReturn(qr64);

        service.processPayment(feesData, Tool.toKafkaHeaders(List.of()));
        verify(paygoPaymentRepository).save(paygoPaymentCaptor.capture());
        PaygoPayment payment = paygoPaymentCaptor.getValue();
        assertThat(payment.getPayGoId()).isNotNull();
        assertThat(payment.getDeviceReference()).isNotNull();
        assertThat(payment.getMerchant()).isEqualTo(merchant);
        assertThat(payment.getCredential()).isEqualTo(credential);
        assertThat(payment.getPendingPayment()).isEqualTo(feesData);
        verify(kafkaSender).sendSubPaymentResult(VENDOR_REF,
                Map.of("payGoId", payment.getPayGoId(), "deviceReference", payment.getDeviceReference(), "qr64", qr64));
    }
}