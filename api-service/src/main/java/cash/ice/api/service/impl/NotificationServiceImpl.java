package cash.ice.api.service.impl;

import cash.ice.api.service.NotificationService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.EmailRequest;
import cash.ice.common.dto.SmsMessage;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.sqldb.entity.EmailTemplate;
import cash.ice.sqldb.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final KafkaSender kafkaSender;
    private final EmailTemplateRepository emailTemplateRepository;

    @Value("${ice.cash.registration.sms-pin-message}")
    protected String smsPinMessage;

    @Override
    public void sendSmsPinCode(String pin, String mobile) {
        if (mobile != null) {
            log.debug("Sending PIN message: {}, to {}", String.format(smsPinMessage, "*".repeat(pin.length())), mobile);
            kafkaSender.sendSmsNotification(new SmsMessage(String.format(smsPinMessage, pin), List.of(mobile)));
        } else {
            throw new ICEcashException("Cannot send PIN, msisdn is absent", ErrorCodes.EC1046);
        }
    }

    public void sendSmsMessage(String message, String mobile) {
        if (mobile != null) {
            log.debug("Sending SMS message to: {}: \n{}", mobile, message);
            kafkaSender.sendSmsNotification(new SmsMessage(message, List.of(mobile)));
        } else {
            throw new ICEcashException("Cannot send SMS, msisdn is absent", ErrorCodes.EC1046);
        }
    }

    @Override
    public boolean sendEmail(EmailRequest emailRequest) {
        log.debug("Sending email from: {}, to: {}, subject: {}, body: {}", emailRequest.getFrom(),
                emailRequest.getRecipients().get(0).getEmailAddress(), emailRequest.getSubject(),
                trancateString(emailRequest.getMessageBody(), 30));
        kafkaSender.sendEmailNotification(emailRequest);
        return true;
    }

    private String trancateString(String str, int maxLength) {
        return str != null && str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    @Override
    public boolean sendEmailByTemplate(boolean sendEmail, String emailTemplateKey, int languageId, String emailFrom, List<String> emailsTo, Map<String, String> bodyReplacers) {
        EmailTemplate emailTemplate = emailTemplateRepository.findByTypeAndLanguageId(emailTemplateKey, languageId)
                .orElseThrow(() -> new ICEcashException(String.format("Unknown email template: %s for language ID: %s",
                        emailTemplateKey, languageId), ErrorCodes.EC1040));
        String body = bodyReplacers.entrySet().stream()
                .map(replacer -> (Function<String, String>) s -> s.replace(replacer.getKey(), replacer.getValue()))
                .reduce(Function.identity(), Function::andThen)
                .apply(emailTemplate.getBody());
        if (sendEmail) {
            return sendEmail(new EmailRequest()
                    .setFrom(emailFrom)
                    .setSubject(emailTemplate.getSubject())
                    .setMessageBody(body)
                    .setRecipients(emailsTo.stream().map(e -> new EmailRequest.Recipient().setEmailAddress(e)).toList()));
        } else {
            return false;
        }
    }
}
