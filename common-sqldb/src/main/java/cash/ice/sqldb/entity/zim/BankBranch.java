package cash.ice.sqldb.entity.zim;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "bank_branch")
@Data
@Accessors(chain = true)
public class BankBranch implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "bank_id", nullable = false)
    private Integer bankId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "branch_no")
    private Integer branchNo;

    @Column(name = "branch_no_fixed")
    private Integer branchNoFixed;

    @Column(name = "flexcube_code")
    private String flexcubeCode;

    @Column(name = "swift_code")
    private String swiftCode;

    @Column(name = "icecash_account_number")
    private String icecashAccountNumber;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "legacy_bank_id")
    private Integer legacyBankId;

    @Column(name = "visible", nullable = false)
    private boolean visible;
}
