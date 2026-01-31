package cash.ice.api.entity.backoffice;

import cash.ice.common.utils.Tool;
import cash.ice.sqldb.converter.JsonToMapConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "journal")
@Data
@Accessors(chain = true)
public class Journal implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private JournalStatus status;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "dr_account_id", nullable = false)
    private Integer drAccountId;

    @Column(name = "cr_account_id", nullable = false)
    private Integer crAccountId;

    @Column(name = "transaction_code_id", nullable = false)
    private Integer transactionCodeId;

    @Column(name = "currency_id", nullable = false)
    private Integer currencyId;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "fees_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Object fees;

    @Column(name = "details")
    private String details;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "created_by_staff_id")
    private Integer createdByStaffId;

    @Column(name = "action_date")
    private LocalDateTime actionDate;

    @Column(name = "action_by_staff_id")
    private Integer actionByStaffId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @JsonIgnore
    public List<Map<String, Object>> getFeesData() {
        return (List<Map<String, Object>>) fees;
    }

    public Journal setFeesData(String jsonString) {
        this.fees = Tool.jsonStringToListMap(jsonString);
        return this;
    }
}
