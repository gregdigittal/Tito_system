package cash.ice.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.kafka.topic")
@Component
@Data
public class KafkaTopicsProperties {
    private int partitions;
    private int replicas;
    private String paymentRequest;
    private String feeService;
    private String paymentSelector;
    private String subPaymentResult;
    private String paygoService;
    private String paygoSuccessPayment;
    private String ecocashService;
    private String ecocashSuccessPayment;
    private String onemoneyService;
    private String onemoneySuccessPayment;
    private String flexcubeLedgerService;
    private String flexcubeLedgerSuccessPayment;
    private String rtgsService;
    private String rtgsSuccessPayment;
    private String zipitService;
    private String zipitSuccessPayment;
    private String zimSwitchService;
    private String zimSwitchSuccessPayment;
    private String mpesaService;
    private String mpesaSuccessPayment;
    private String emolaService;
    private String emolaSuccessPayment;
    private String zimPaymentRequest;
    private String zimPaymentResult;
    private String zimPaymentError;
    private String posbService;
    private String fbcService;
    private String fcbService;
    private String ledgerService;
    private String successPayment;
    private String errorPayment;
    private String afterSuccessPayment;
    private String balanceAudit;
    private String emailNotification;
    private String smsNotification;
    private String invalidateCache;
}
