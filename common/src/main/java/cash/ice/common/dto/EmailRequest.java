package cash.ice.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class EmailRequest {
    private String from;
    private String subject;
    private String messageBody;
    private List<Recipient> recipients;

    @Data
    @Accessors(chain = true)
    public static class Recipient {
        private String name;
        private String emailAddress;
    }
}
