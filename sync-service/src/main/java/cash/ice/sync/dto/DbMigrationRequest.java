package cash.ice.sync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DbMigrationRequest {
    private Boolean migrateAll;
    private List<String> migrateServices;
}
