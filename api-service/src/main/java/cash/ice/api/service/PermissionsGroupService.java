package cash.ice.api.service;

import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;

import java.util.List;

public interface PermissionsGroupService {

    void grantMozPermissionsToAccounts(List<Integer> securityGroupIds, EntityClass entity, Account... accounts);

    void grantKenPermissionsToAccounts(List<Integer> securityGroupIds, EntityClass entity, Account... accounts);

    boolean hasUserMozSecurityGroup(EntityClass entity, Integer securityGroupId);

    boolean hasUserKenSecurityGroup(EntityClass entity, Integer securityGroupId);

    void validateUserMozSecurityGroup(EntityClass entity, Integer securityGroupId);

    void validateUserKenSecurityGroup(EntityClass entity, Integer securityGroupId);
}
