package cash.ice.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SmsMessage {
    private String messageBody;
    private List<String> mobileNumbers;
}
