package cash.ice.api.service;

import cash.ice.common.dto.EmailRequest;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    void sendSmsPinCode(String pin, String mobile);

    void sendSmsMessage(String message, String mobile);

    boolean sendEmail(EmailRequest emailRequest);

    boolean sendEmailByTemplate(boolean sendEmail, String emailTemplateKey, int languageId, String emailFrom, List<String> emailsTo, Map<String, String> bodyReplacers);
}
