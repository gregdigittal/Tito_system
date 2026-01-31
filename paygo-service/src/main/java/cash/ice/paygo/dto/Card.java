package cash.ice.paygo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Card {
    private LocalDate expiry;
    private String pan;
}
