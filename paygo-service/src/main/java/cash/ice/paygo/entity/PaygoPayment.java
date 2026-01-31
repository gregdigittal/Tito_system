package cash.ice.paygo.entity;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.paygo.dto.admin.Credential;
import cash.ice.paygo.dto.admin.Merchant;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "PaygoPendingRequest")
@Data
@Accessors(chain = true)
public class PaygoPayment {

    @Id
    private String id;
    @Indexed(name = "expirationIndex", expireAfterSeconds = 3600)
    private Date created = new Date();

    private String payGoId;
    private String deviceReference;
    private Merchant merchant;
    private Credential credential;
    private int expirySeconds;
    private FeesData pendingPayment;
    private List<String> kafkaHeaders;
}
