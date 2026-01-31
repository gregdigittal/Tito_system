package cash.ice.paygo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Origin {
    private String contactNumber;
    private String emailAddress;
    private String firstName;
    private String lastName;
    private String nationalId;
    private String type;
}
