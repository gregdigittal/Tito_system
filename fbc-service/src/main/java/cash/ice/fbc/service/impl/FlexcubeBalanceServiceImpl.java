package cash.ice.fbc.service.impl;

import cash.ice.common.dto.EmailRequest;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.dto.flexcube.FlexcubeBalanceResponse;
import cash.ice.fbc.entity.FlexcubeAccount;
import cash.ice.fbc.entity.FlexcubePayment;
import cash.ice.fbc.error.FlexcubeException;
import cash.ice.fbc.service.FlexcubeBalanceService;
import cash.ice.fbc.service.FlexcubeSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

import static cash.ice.common.error.ErrorCodes.EC8004;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlexcubeBalanceServiceImpl implements FlexcubeBalanceService {
    private final FlexcubeProperties flexcubeProperties;
    private final FlexcubeSenderService flexcubeSenderService;
    private final KafkaSender kafkaSender;

    @Override
    public FlexcubeBalanceResponse checkBalance(FlexcubeAccount fbcPoolAccount, BigDecimal amount, FlexcubePayment payment) {
        FlexcubeBalanceResponse response = getBalance(fbcPoolAccount.getDebitPoolAccount(), fbcPoolAccount.getDebitPoolAccountBranch(), payment);
        if (response.getAvailableBalance().compareTo(
                amount.add(fbcPoolAccount.getTransactionFee()).add(fbcPoolAccount.getMinBalanceMargin())) < 0) {
            throw new FlexcubeException(payment, "Insufficient balance: " + response.getAvailableBalance() + " to pay " +
                    amount, EC8004);
        }
        if (response.getAvailableBalance().compareTo(fbcPoolAccount.getBalanceWarningValue()) < 0) {
            sendBalanceWarning(fbcPoolAccount, response.getAvailableBalance());
        }
        return response;
    }

    @Override
    public void updateBalanceCache(FlexcubeBalanceResponse balance, FlexcubeAccount fbcPoolAccount, FlexcubePayment payment) {
        balance.setAvailableBalance(balance.getAvailableBalance()
                .subtract(fbcPoolAccount.getTransactionFee())
                .subtract(payment.getPendingPayment().getPaymentRequest().getAmount()));
        balance.setLedgerBalance(balance.getLedgerBalance()
                .subtract(fbcPoolAccount.getTransactionFee())
                .subtract(payment.getPendingPayment().getPaymentRequest().getAmount()));
        balance = flexcubeSenderService.updateBalance(balance);
    }

    private FlexcubeBalanceResponse getBalance(String account, String branch, FlexcubePayment payment) {
        try {
            FlexcubeBalanceResponse balance = flexcubeSenderService.getBalance(account, branch);
            if (Duration.between(balance.getCreatedTime(), Instant.now())
                    .compareTo(flexcubeProperties.getBalanceCacheDuration()) > 0) {
                flexcubeSenderService.evictBalance(account, branch);
                FlexcubeBalanceResponse latestBalance = flexcubeSenderService.getBalance(account, branch);
                if (balance.getAvailableBalance().equals(latestBalance.getAvailableBalance())) {
                    log.warn("Server and saved Balances are not equal, probably transaction fee was changed!");
                }
                balance = latestBalance;
            }
            return balance;
        } catch (ICEcashException e) {
            throw new FlexcubeException(payment, e.getMessage(), e.getErrorCode());
        }
    }

    private void sendBalanceWarning(FlexcubeAccount fbcPoolAccount, BigDecimal availableBalance) {
        log.info("Warning email, FBC Pool account balance is small. account: {}, branch: {}, balance: {}",
                fbcPoolAccount.getDebitPoolAccount(), fbcPoolAccount.getDebitPoolAccountBranch(), availableBalance);
        EmailRequest emailRequest = new EmailRequest()
                .setFrom(flexcubeProperties.getBalanceWarningEmail().getFrom())
                .setSubject(flexcubeProperties.getBalanceWarningEmail().getSubject())
                .setMessageBody(String.format(flexcubeProperties.getBalanceWarningEmail().getBody(),
                        fbcPoolAccount.getDebitPoolAccount(),
                        fbcPoolAccount.getDebitPoolAccountBranch(),
                        availableBalance))
                .setRecipients(Stream.of(flexcubeProperties.getBalanceWarningEmail().getRecipients().split(",")).map(str -> {
                            if (ObjectUtils.isEmpty(str)) {
                                return null;
                            } else {
                                EmailRequest.Recipient recipient = new EmailRequest.Recipient()
                                        .setEmailAddress(StringUtils.substringBetween(str, "<", ">"));
                                if (recipient.getEmailAddress() != null) {
                                    recipient.setName(str.substring(0, str.indexOf("<")).strip());
                                } else {
                                    recipient.setEmailAddress(str.strip());
                                }
                                return recipient;
                            }
                        }
                ).filter(Objects::nonNull).toList());
        if (!emailRequest.getRecipients().isEmpty()) {
            kafkaSender.sendEmailNotification(emailRequest);
        } else {
            log.warn("  recipients list is empty: ");
        }
    }
}
