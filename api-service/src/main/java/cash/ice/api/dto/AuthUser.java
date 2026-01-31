package cash.ice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class AuthUser {
    private String principal;
    private String realm = "ONLINE";        // todo
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<String> roles = new HashSet<>();

    public boolean isStaffMember() {
        return roles.contains("ROLE_BACKOFFICE");
    }
}
