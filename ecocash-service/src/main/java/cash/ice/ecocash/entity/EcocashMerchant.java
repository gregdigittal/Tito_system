package cash.ice.ecocash.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "EcocashMerchant")
@Data
@Accessors(chain = true)
public class EcocashMerchant {

    @Id
    private String id;

    private List<String> transactionCodes;
    private Boolean general;
    private String pin;
    private String code;
    private String number;
}
