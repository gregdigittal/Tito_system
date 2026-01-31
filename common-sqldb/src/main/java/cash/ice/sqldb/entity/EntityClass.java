package cash.ice.sqldb.entity;

import cash.ice.common.constant.EntityMetaKey;
import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.sqldb.converter.StringListConverter;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Entity
@Table(name = "entity")
@Data
@Accessors(chain = true)
public class EntityClass implements Serializable {
    public static final int INTERNAL_NUM_LENGTH = 16;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "id_type")
    private Integer idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "pin_key")
    private String internalId;

    @Column(name = "pvv")
    private String pvv;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "citizenship")
    private Integer citizenshipCountryId;

    @Column(name = "email")
    private String email;

    @Column(name = "login_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LoginStatus loginStatus;

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

    @Column(name = "keycloak_id")
    private String keycloakId;

    @Column(name = "reg_by_entity_id")
    private Integer regByEntityId;

    @Column(name = "legacy_account_id")
    private Integer legacyAccountId;

    @Column(name = "kyc_status_id", nullable = false)
    private int kycStatusId;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }

    public String keycloakUsername() {
        return String.valueOf(legacyAccountId != null ? legacyAccountId : (30000000 + id));
    }

    public String idString() {
        return String.valueOf(id);
    }

    public EntityClass updateData(EntityClass entity) {
        return setEntityTypeId(entity.entityTypeId)
                .setFirstName(entity.firstName)
                .setLastName(entity.lastName)
                .setIdType(entity.idType)
                .setIdNumber(entity.idNumber)
                .setGender(entity.gender)
                .setStatus(entity.status)
                .setBirthDate(entity.birthDate)
                .setCitizenshipCountryId(entity.citizenshipCountryId)
                .setEmail(entity.email)
                .setMfaType(entity.mfaType)
                .setLocale(entity.locale);
    }

    public String getMetaTierString() {
        if (meta != null) {
            LimitTier tier = (LimitTier) meta.get(EntityMetaKey.TransactionLimitTier);
            if (tier != null) {
                return tier.name();
            }
        }
        return null;
    }
}
