package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.dto.SortInput;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.sqldb.entity.Language;
import cash.ice.sqldb.entity.LoginStatus;
import cash.ice.sqldb.entity.MfaType;
import org.springframework.lang.Nullable;

import java.util.function.Supplier;

public interface StaffMemberService {

    StaffMember getAuthStaffMember(AuthUser authUser, ConfigInput config);

    StaffMember getStaffMemberById(Integer id);

    Iterable<StaffMember> searchStaffMembers(String searchText, LoginStatus status, int page, int size, SortInput sort);


    String getUsersCsv(String searchText, LoginStatus status, boolean header, Character delimiter, String rowDelimiter);

    StaffMember createNewStaffMember(StaffMember staffMember, String url, boolean sendEmail);

    boolean isExistsId(Integer idTypeId, String idNumber);

    boolean isExistsEmail(String email);

    StaffMember updateStaffMember(StaffMember staffMember, StaffMember staffMemberDetails, @Nullable StaffMember updater, ConfigInput config, boolean sendEmail);

    StaffMember updateMsisdn(StaffMember staffMember, String msisdn);

    StaffMember updateMfaType(StaffMember staffMember, MfaType mfaType);

    StaffMember generateNewBackupCodes(Integer id);

    StaffMember generateNewBackupCodes(AuthUser authUser);

    StaffMember deleteStaffMember(Integer id);

    StaffMember deleteStaffMember(AuthUser authUser);

    StaffMember save(StaffMember staffMember);

    StaffMember findActiveStaffMember(String email);

    StaffMember findStaffMember(String email);

    StaffMember findStaffMemberOrElse(String email, Supplier<StaffMember> staffMemberSupplier);

    boolean isStaffMemberExist(String email);

    Language getStaffMemberLanguage(StaffMember staffMember);

    String generateStaffMemberPinKey();
}
