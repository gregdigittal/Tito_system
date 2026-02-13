package cash.ice.sqldb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Phase 8-3: Bank account linked by entity (Sacco, owner, agent) for settlements/sweep.
 */
@Entity
@Table(name = "linked_bank_account")
@Data
@Accessors(chain = true)
public class LinkedBankAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "bank_id", nullable = false)
    private String bankId;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LinkedBankAccountStatus status;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
