package cash.ice.fbc.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.service.KafkaSender;
import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.dto.flexcube.FlexcubeBalanceResponse;
import cash.ice.fbc.dto.flexcube.FlexcubePaymentRequest;
import cash.ice.fbc.dto.flexcube.FlexcubeResponse;
import cash.ice.fbc.dto.flexcube.FlexcubeStatusRequest;
import cash.ice.fbc.entity.FlexcubeAccount;
import cash.ice.fbc.entity.FlexcubePayment;
import cash.ice.fbc.error.FlexcubeTimeoutException;
import cash.ice.fbc.repository.FlexcubeAccountRepository;
import cash.ice.fbc.repository.FlexcubePaymentRepository;
import cash.ice.fbc.service.FlexcubeBalanceService;
import cash.ice.fbc.service.FlexcubeSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class FlexcubeServiceUatImpl extends FlexcubeServiceImpl {

    public FlexcubeServiceUatImpl(FlexcubeProperties flexcubeProperties, FlexcubeBalanceService flexcubeBalanceService, FlexcubeSenderService flexcubeSenderService, FlexcubeAccountRepository flexcubeAccountRepository, FlexcubePaymentRepository flexcubePaymentRepository, KafkaSender kafkaSender) {
        super(flexcubeProperties, flexcubeBalanceService, flexcubeSenderService, flexcubeAccountRepository, flexcubePaymentRepository, kafkaSender);
    }

    @Override
    protected FlexcubeBalanceResponse checkBalance(FlexcubeAccount fbcPoolAccount, BigDecimal amount, FlexcubePayment payment) {
        if (payment.getPendingPayment().getPaymentRequest().getMeta().containsKey("simulate")) {
            return new FlexcubeBalanceResponse()
                    .setResultCode("00")
                    .setResultDescription("success")
                    .setAccount(fbcPoolAccount.getDebitPoolAccount())
                    .setBranch(fbcPoolAccount.getDebitPoolAccountBranch())
                    .setAvailableBalance(new BigDecimal("900000000"))
                    .setLedgerBalance(new BigDecimal("900000000"));
        } else {
            return super.checkBalance(fbcPoolAccount, amount, payment);
        }
    }

    @Override
    protected FlexcubeResponse sendPayment(FlexcubePaymentRequest request, FlexcubePayment payment) {
        String simType = (String) payment.getPendingPayment().getPaymentRequest().getMeta().get("simulate");
        if (simType != null) {
            return switch (simType) {
                case "00" -> new FlexcubeResponse().setResultCode("00").setResultDescription("simulated success");
                case "01" -> new FlexcubeResponse().setResultCode("01").setResultDescription("simulated error");
                case "02", "03" -> throw new FlexcubeTimeoutException(request.getExternalReference());
                default -> throw new IllegalArgumentException("Unknown 'simulate' type!");
            };
        } else {
            return super.sendPayment(request, payment);
        }
    }

    @Override
    protected FlexcubeResponse sendCheck(FlexcubeStatusRequest request, FlexcubePayment payment) {
        String simType = (String) payment.getPendingPayment().getPaymentRequest().getMeta().get("simulate");
        if (simType != null) {
            return switch (simType) {
                case "00", "02" -> new FlexcubeResponse().setResultCode("00").setResultDescription("simulated success");
                case "01", "03" -> new FlexcubeResponse().setResultCode("01").setResultDescription("simulated error");
                default -> throw new IllegalArgumentException("Unknown 'simulate' type!");
            };
        } else {
            return super.sendCheck(request, payment);
        }
    }

    @Override
    protected void updateBalanceCache(FlexcubeBalanceResponse balanceResponse, FlexcubeAccount fbcPoolAccount, FlexcubePayment payment) {
        if (!payment.getPendingPayment().getPaymentRequest().getMeta().containsKey("simulate")) {
            super.updateBalanceCache(balanceResponse, fbcPoolAccount, payment);
        }
    }
}
