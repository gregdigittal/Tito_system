package cash.ice.sqldb.entity;

import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "Transaction")
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "statement_date", nullable = false)
    private LocalDateTime statementDate;

    @Column(name = "initiator_id")
    private Integer initiatorId;

    @Column(name = "initiator_type_id", nullable = false)
    private Integer initiatorTypeId;

    @Column(name = "currency_id", nullable = false)
    private Integer currencyId;

    @Column(name = "transaction_code_id", nullable = false)
    private Integer transactionCodeId;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }
}
