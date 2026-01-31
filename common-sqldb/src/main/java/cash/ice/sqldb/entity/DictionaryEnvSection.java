package cash.ice.sqldb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DictionaryEnvSection {
    private Integer languageId;
    private String environment;
    private String section;
}
