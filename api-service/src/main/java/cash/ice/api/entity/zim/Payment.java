package cash.ice.api.entity.zim;

import cash.ice.api.converter.JsonToPaymentDocumentConverter;
import cash.ice.api.dto.PaymentDocument;
import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.sqldb.entity.AccountSide;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "payment")
@Data
@Accessors(chain = true)
public class Payment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(name = "account_side", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountSide accountSide;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "description")
    private String description;

    @Column(name = "count")
    private Integer count;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "tax_declaration_id")
    private Integer taxDeclarationId;

    @Column(name = "tax_reason_id")
    private Integer taxReasonId;

    @Column(name = "payment_collection_id")
    private Integer paymentCollectionId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "documents", columnDefinition = "json")
    @Convert(converter = JsonToPaymentDocumentConverter.class)
    private List<PaymentDocument> documents;

    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;

    public Optional<PaymentDocument> getDocumentById(String documentId) {
        return getDocuments().stream().filter(
                doc -> doc.getDocumentId().equals(documentId)).findAny();
    }

    public Payment addMetaField(String key, Object val) {
        if (meta == null) {
            meta = new HashMap<>();
        }
        meta.put(key, val);
        return this;
    }
}
