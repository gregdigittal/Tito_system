package cash.ice.fee.service.impl;

import cash.ice.fee.dto.PublicApiPayment;
import cash.ice.fee.component.ICEcashDateTimeFormatter;
import cash.ice.fee.dto.group.*;
import cash.ice.fee.error.ICEcashGroupApiException;
import cash.ice.fee.error.ICEcashInvalidRequestException;
import cash.ice.fee.service.GroupApiPaymentQueryService;
import cash.ice.fee.service.GroupApiPaymentService;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Date;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@AllArgsConstructor
@Slf4j
public class GroupApiPaymentServiceImpl implements GroupApiPaymentService {
    private Gson gson;
    private GroupApiPaymentQueryService groupApiPaymentQueryService;
    private ICEcashDateTimeFormatter dateTimeFormatter;

    @Override
    public GroupApiPaymentResponse callGroupApiPayment(@Valid PublicApiPayment apiPayment) throws IOException {
        log.info("sending publicApiPayment: " + apiPayment);
        GroupApiPaymentRequest paymentRequest = this.translatePaymentRequest(apiPayment);
        log.debug("paymentRequest: " + paymentRequest);
        String rawResponse = groupApiPaymentQueryService.query(paymentRequest);
        log.debug("publicApiPayment response: " + rawResponse);
        GroupApiPaymentResponse paymentResponse = gson.fromJson(rawResponse, GroupApiPaymentResponse.class);
        log.debug("paymentResponse: " + paymentResponse);
        checkResponse(paymentResponse, rawResponse);
        return paymentResponse;
    }

    private GroupApiPaymentRequest translatePaymentRequest(@Valid PublicApiPayment apiPayment) {
        String thirdPartyPayment = this.translateThirdPartyOption(apiPayment.getInitiatorType());
        String identifierType = this.translateIdentifierType(thirdPartyPayment);

        return new GroupApiPaymentRequest(
                new GroupApiPaymentRequestBody(
                        "2.2",
                        dateTimeFormatter.formatForGroupApi(new Date(apiPayment.getDate().toInstant(ZoneOffset.UTC).toEpochMilli())),
                        apiPayment.getVendorRef(),
                        "",
                        "",
                        apiPayment.getTx(),
                        thirdPartyPayment,
                        "",
                        "",
                        "",
                        identifierType,
                        apiPayment.getInitiator(),
                        apiPayment.getInitiatorPVV(),
                        "",
                        "",
                        apiPayment.getInitiatorTrack2(),
                        apiPayment.getDeviceId(),
                        "1",
                        "",
                        "",
                        "",
                        "1",
                        apiPayment.getAmount().toString(),
                        "",
                        "",
                        "",
                        new GroupApiPaymentTxAnalysis(
                                "", ""
                        ),
                        new GroupApiAgent(
                                "", apiPayment.getDeviceUser()
                        )
                )
        );
    }

    private void checkResponse(GroupApiPaymentResponse paymentResponse, String rawResponse) {
        if (paymentResponse.getPaymentResponse() == null) {
            GroupApiError groupApiError = gson.fromJson(rawResponse, GroupApiError.class);
            if (groupApiError.getError().getError() == null) {
                throw new ICEcashGroupApiException("Unreadable response from GroupApi", EC3008);
            } else {
                throw new ICEcashGroupApiException(groupApiError.getError().getError(), groupApiError.getError().getResult(), EC3009);
            }
        } else if (!paymentResponse.getPaymentResponse().getResult().equalsIgnoreCase("1")) {
            throw new ICEcashGroupApiException(paymentResponse.getPaymentResponse().getMessage(),
                    paymentResponse.getPaymentResponse().getResult(), EC3010);
        }
    }

    private String translateIdentifierType(String paymentMethod) {
        if (paymentMethod.equalsIgnoreCase("creditcard") || paymentMethod.equalsIgnoreCase("zimswitch")) {
            return "Card";
        } else if (paymentMethod.equalsIgnoreCase("icecash") || paymentMethod.equalsIgnoreCase("ecocash") ||
                paymentMethod.equalsIgnoreCase("netone")) {
            return "Mobile";
        } else {
            throw new ICEcashInvalidRequestException("Invalid payment method", EC3007);
        }
    }

    private String translateThirdPartyOption(String input) {
        if (input.equalsIgnoreCase("creditcard"))
            return "CreditCard";
        else if (input.equalsIgnoreCase("ecocash"))
            return "EcoCash";
        else if (input.equalsIgnoreCase("icecash"))
            return "ICEcash";
        else if (input.equalsIgnoreCase("netone"))
            return "NetOne";
        else if (input.equalsIgnoreCase("zimswitch"))
            return "ZimSwitch";
        else if (input.equalsIgnoreCase("cash"))
            return "Cash";
        else
            throw new ICEcashInvalidRequestException("Invalid payment method", EC3006);
    }

}
