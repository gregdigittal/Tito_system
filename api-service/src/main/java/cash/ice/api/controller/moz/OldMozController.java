package cash.ice.api.controller.moz;

import cash.ice.api.dto.moz.LinkNfcTagRequest;
import cash.ice.api.dto.moz.TagLinkResponse;
import cash.ice.api.service.Me60MozService;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Initiator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OldMozController {
    private final Me60MozService me60MozService;

    @MutationMapping
    public String linkTagStartMoz(@Argument String device, @Argument String accountNumber, @Argument LocalDateTime dateTime) {
        log.info("> link tag (1st step). Device: {}, AccountNumber: {}, dateTime: {}", device, accountNumber, dateTime);
        return me60MozService.linkTag(new LinkNfcTagRequest()
                .setDeviceSerial(device)
                .setAccountNumber(accountNumber)
                .setDateTime(dateTime != null ? dateTime : Tool.currentDateTime()));
    }

    @MutationMapping
    public TagLinkResponse linkTagValidateOtpMoz(@Argument String requestId, @Argument String otp) {
        log.info("> link tag OTP (2nd step). RequestId: {}, OTP: {}", requestId, otp);
        return me60MozService.linkTagValidateOtp(new LinkNfcTagRequest().setRequestId(requestId).setOtp(otp));
    }

    @MutationMapping
    public Initiator linkTagRegisterTagMoz(@Argument String requestId, @Argument String tagNumber) {
        log.info("> link tag register (3rd step). RequestId: {}, tagNumber: {}", requestId, tagNumber);
        return me60MozService.linkTagRegister(new LinkNfcTagRequest().setRequestId(requestId).setTagNumber(tagNumber));
    }
}
