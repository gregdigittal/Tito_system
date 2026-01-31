package cash.ice.ledger.config;

import cash.ice.common.dto.EmailRequest;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "balance-warning-email")
@Component
@Data
@Accessors(chain = true)
public class BalanceWarningEmailProperties {
    private String from;
    private String subject;
    private String body;
    private String recipients;

    public EmailRequest createEmailRequest(Integer accountId, BigDecimal correctBalance, BigDecimal actualBalance) {
        return new EmailRequest()
                .setFrom(from)
                .setSubject(subject)
                .setMessageBody(String.format(body, accountId, correctBalance, actualBalance))
                .setRecipients(Stream.of(recipients.split(",")).map(str -> {
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
    }
}
