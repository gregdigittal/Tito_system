package cash.ice.api.parser;

import cash.ice.api.parser.impl.FbcTemplateParser;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static cash.ice.api.parser.impl.FbcTemplateParser.RTGS_TEMPLATE;

@Component
@RequiredArgsConstructor
public class PaymentsBulkParserFactory {
    private final FbcTemplateParser fbcTemplateParser;

    public PaymentsBulkParser getParser(String template) {
        if (template == null) {
            throw new ICEcashException("Payment template must be provided", ErrorCodes.EC1030);
        }
        return switch (template) {
            case RTGS_TEMPLATE -> fbcTemplateParser;
            default -> throw new ICEcashException("Unknown payment template: " + template, ErrorCodes.EC1030);
        };
    }
}
