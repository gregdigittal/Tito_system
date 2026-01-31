package cash.ice.paygo.entity;

import cash.ice.paygo.dto.admin.Credential;
import cash.ice.paygo.dto.admin.Merchant;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "PaygoMerchant")
@Data
@Accessors(chain = true)
public class PaygoMerchant {

    @Id
    private String id;

    private Merchant merchant;
    private List<Credential> credentials = new ArrayList<>();
}
