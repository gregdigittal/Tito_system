package cash.ice.api.entity.backoffice;

import cash.ice.sqldb.converter.StringListConverter;
import cash.ice.sqldb.entity.LoginStatus;
import cash.ice.sqldb.entity.MfaType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "staff")
@Data
@Accessors(chain = true)
public class StaffMember implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "id_type")
    private Integer idNumberType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "msisdn", nullable = false)
    private String msisdn;

    @Column(name = "department")
    private String department;

    @Column(name = "login_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LoginStatus loginStatus;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "pin_key")
    private String pinKey;

    @Column(name = "pvv")
    private String pvv;

    @Column(name = "keycloak_id")
    private String keycloakId;

    @Column(name = "security_group_id")
    private Integer securityGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type")
    private MfaType mfaType;

    @Column(name = "mfa_secret_code")
    private String mfaSecretCode;

    @Convert(converter = StringListConverter.class)
    @Column(name = "mfa_backup_codes")
    private List<String> mfaBackupCodes;

    @Column(name = "locale")
    private Locale locale;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public StaffMember updateData(StaffMember newStaffMemberData) {
        email = newStaffMemberData.email;
        firstName = newStaffMemberData.firstName;
        lastName = newStaffMemberData.lastName;
        idNumberType = newStaffMemberData.idNumberType;
        idNumber = newStaffMemberData.idNumber;
        msisdn = newStaffMemberData.msisdn;
        department = newStaffMemberData.department;
        entityId = newStaffMemberData.entityId;
        securityGroupId = newStaffMemberData.securityGroupId;
        mfaType = newStaffMemberData.mfaType;
        locale = newStaffMemberData.locale;
        loginStatus = newStaffMemberData.loginStatus;
        return this;
    }
}
