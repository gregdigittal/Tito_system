package cash.ice.api.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.EmailRequest;
import cash.ice.common.service.KafkaSender;
import cash.ice.sqldb.repository.EmailTemplateRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
@Setter
@ConfigurationProperties(prefix = "ice.cash.notification")
public class NotificationServiceUatImpl extends NotificationServiceImpl {
    private List<String> fakeSmsMobiles;
    private List<String> fakeEmails;

    public NotificationServiceUatImpl(KafkaSender kafkaSender, EmailTemplateRepository emailTemplateRepository) {
        super(kafkaSender, emailTemplateRepository);
    }

    @Override
    public void sendSmsPinCode(String pin, String mobile) {
        if (fakeSmsMobiles.contains(mobile)) {
            log.debug("Skipping send PIN message: {}, to {}", String.format(smsPinMessage, "*".repeat(pin.length())), mobile);
        } else {
            super.sendSmsPinCode(pin, mobile);
        }
    }

    @Override
    public void sendSmsMessage(String message, String mobile) {
        if (fakeSmsMobiles.contains(mobile)) {
            log.debug("Skipping send SMS message to: {}: \n{}", mobile, message);
        } else {
            super.sendSmsMessage(message, mobile);
        }
    }

    @Override
    public boolean sendEmail(EmailRequest emailRequest) {
        if (fakeEmails.contains(emailRequest.getFrom()) || emailRequest.getRecipients().stream()
                .map(EmailRequest.Recipient::getEmailAddress)
                .anyMatch(email -> fakeEmails.contains(email))) {
            log.debug("Skipping send email from: {}, to: {}, subject: {}, body: {}", emailRequest.getFrom(),
                    emailRequest.getRecipients().get(0).getEmailAddress(), emailRequest.getSubject(), emailRequest.getMessageBody());
            return false;
        } else {
            return super.sendEmail(emailRequest);
        }
    }
}
