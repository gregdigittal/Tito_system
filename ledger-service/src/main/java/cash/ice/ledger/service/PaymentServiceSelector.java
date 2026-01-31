package cash.ice.ledger.service;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static cash.ice.common.error.ErrorCodes.EC4004;

@Service
@RequiredArgsConstructor
public class PaymentServiceSelector {
    private static final String FBC_BANK_BIN = "601704";
    private static final String RTG = "RTG";
    private static final String TRN = "TRN";


    private final KafkaSender kafkaSender;

    public void selectAndSendToServiceTopic(String initiatorTypeDescription, String vendorRef, FeesData value) {
        switch (InitiatorTypeEnum.getByDescription(initiatorTypeDescription)) {
            case ECOCASH -> kafkaSender.sendEcocashService(vendorRef, value);
            case CARD -> kafkaSender.sendZimSwitchService(vendorRef, value);
            case PAYGO -> kafkaSender.sendPaygoService(vendorRef, value);
            case NETONE -> kafkaSender.sendOnemoneyService(vendorRef, value);
            case ICECASH -> selectIceCashService(vendorRef, value);
            case MPESA -> kafkaSender.sendMpesaService(vendorRef, value);
            case EMOLA -> kafkaSender.sendEmolaService(vendorRef, value);
            default -> kafkaSender.sendLedgerService(vendorRef, value);
        }
    }

    private void selectIceCashService(String vendorRef, FeesData value) {
        String transactionCode = value.getPaymentRequest().getTx();
        String bankBIN = (String) value.getPaymentRequest().getMeta().get("bankBIN");
        String bankAccountNo = (String) value.getPaymentRequest().getMeta().get("bankAccountNo");
        if (FBC_BANK_BIN.equals(bankBIN)) {
            if (bankAccountNo.length() == 13) {
                kafkaSender.sendFlexcubeLedgerService(vendorRef, value);
            } else {
                kafkaSender.sendZipitService(vendorRef, value);
            }
        } else if (RTG.equals(transactionCode)) {
            kafkaSender.sendZipitService(vendorRef, value);
        } else if (TRN.equals(transactionCode)) {
            kafkaSender.sendFlexcubeLedgerService(vendorRef, value);
        } else {
            kafkaSender.sendLedgerService(vendorRef, value);
        }
    }

    //----------------------------------------------------------------------------
    public enum InitiatorTypeEnum {
        ICECASH, CARD, ECOCASH, PAYGO, NETONE, TAG, ACCOUNTNUMBER, MPESA, EMOLA, JOURNAL;

        public static InitiatorTypeEnum getByDescription(String initiatorDescription) {
            try {
                return InitiatorTypeEnum.valueOf(initiatorDescription.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ICEcashException("Unknown payment service for InitiatorType: " + initiatorDescription, EC4004);
            }
        }
    }
}
