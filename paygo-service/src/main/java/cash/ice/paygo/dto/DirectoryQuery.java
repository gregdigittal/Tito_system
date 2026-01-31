package cash.ice.paygo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DirectoryQuery {
    private String alias;
    private String type;
}
