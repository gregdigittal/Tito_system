package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;

import java.util.List;

public interface PermissionsService {

    boolean checkReadRights(AuthUser authUser, Integer accountId);

    boolean checkWriteRights(AuthUser authUser, Integer accountId);

    boolean checkWriteRights(EntityClass entityClass, Integer accountId, String realm);

    EntityClass getAuthEntity(AuthUser authUser);

    StaffMember getAuthStaffMember(AuthUser authUser);

    List<String> getRights(EntityClass authEntity, Account account, String realm);

    void checkApprovePermissions(Account account, List<String> rights, String realm, Integer prevApprover);
}
