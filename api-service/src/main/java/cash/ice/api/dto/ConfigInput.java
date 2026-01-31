package cash.ice.api.dto;

import lombok.Data;

@Data
public class ConfigInput {
    private Integer auth;
    private boolean noEmails;
}
