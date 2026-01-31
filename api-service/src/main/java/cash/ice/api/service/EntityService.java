package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.TransactionView;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Locale;

public interface EntityService {

    EntityClass getEntityById(Integer entityId);

    EntityClass getEntity(AuthUser authUser);

    EntityClass updateEntity(AuthUser authUser, EntityClass entityDetails);

    EntityClass updateEntity(Integer entityId, EntityClass details, StaffMember updater);

    EntityClass updateEntityLocale(AuthUser authUser, Locale locale);

    EntityClass generateNewBackupCodes(Integer id);

    EntityClass generateNewBackupCodes(AuthUser authUser);

    EntityClass deleteEntity(AuthUser authUser);

    Language getEntityLanguage(EntityClass entity);

    Iterable<Account> getAccountsFor(EntityClass entity, PageRequest pageRequest);

    Account setAccountActive(EntityClass entity, Integer accountId, boolean active);

    Page<TransactionView> getEntityTransactions(EntityClass authEntity, String accountTypeName, String currencyCode, Integer vrnId, Integer tagId, String transactionCode, String description, int page, int size, SortInput sort);

    String getStatementCsv(EntityClass authEntity, String accountTypeName, String currencyCode, boolean header, Character delimiter, String rowDelimiter);
}
