package cash.ice.emola.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class EmolaRequest {
    private String wscode;
    private Map<String, String> params;

}
