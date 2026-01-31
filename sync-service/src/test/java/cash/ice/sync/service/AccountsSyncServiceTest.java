package cash.ice.sync.service;

import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountStatus;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.AccountTypeRepository;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.common.utils.Tool;
import cash.ice.sync.component.DateTimeParser;
import cash.ice.sync.dto.AccountChange;
import cash.ice.sync.dto.ChangeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int LEGACY_ACCOUNT_ID = 200;
    private static final int ENTITY_ID = 10;
    private static final int ACCOUNT_TYPE_ID = 12;

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private AccountsSyncService service;

    @BeforeEach
    void init() {
        service = new AccountsSyncService(jdbcTemplate, entityRepository, accountRepository, accountTypeRepository,
                currencyRepository, new DateTimeParser());
    }

    @Test
    void testAccountCreate() throws URISyntaxException, IOException {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        AccountChange accountChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/accountRequest.json"), AccountChange.class);
        when(entityRepository.findByLegacyAccountId(LEGACY_ACCOUNT_ID)).thenReturn(
                Optional.of(entity));
        when(accountTypeRepository.findByLegacyWalletId("1")).thenReturn(
                Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        service.update(accountChange);
        checkAccount(entity);
    }

    private void checkAccount(EntityClass entity) {
        verify(accountRepository).save(accountCaptor.capture());
        Account actualAccount = accountCaptor.getValue();
        assertThat(actualAccount.getEntityId()).isEqualTo(entity.getId());
        assertThat(actualAccount.getAccountTypeId()).isEqualTo(ACCOUNT_TYPE_ID);
        assertThat(actualAccount.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(actualAccount.getCreatedDate().toString()).isEqualTo("2018-04-16T15:43:45");
        assertThat(actualAccount.getDailyLimit()).isEqualTo(new BigDecimal("100.55"));
        assertThat(actualAccount.getOverdraftLimit()).isEqualTo(new BigDecimal("50.23"));
        assertThat(actualAccount.getBalanceMinimum()).isEqualTo(new BigDecimal("10.81"));
        assertThat(actualAccount.getBalanceWarning()).isEqualTo(new BigDecimal("15"));
        assertThat(actualAccount.isBalanceMinimumEnforce()).isTrue();
        assertThat(actualAccount.isNotificationEnabled()).isTrue();
        assertThat(actualAccount.isAutoDebit()).isTrue();
    }

    @Test
    void testAccountUpdate() throws URISyntaxException, IOException {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        AccountChange accountChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/accountRequest.json"), AccountChange.class);
        when(accountRepository.findByAccountNumber("200001")).thenReturn(
                List.of(new Account().setAccountNumber("200001").setEntityId(ENTITY_ID).setAccountTypeId(ACCOUNT_TYPE_ID)));
        service.update(accountChange);
        checkAccount(entity);
    }

    @Test
    void testAccountDelete() {
        AccountChange accountChange = new AccountChange().setAction(ChangeAction.DELETE)
                .setLegacyAccountId(LEGACY_ACCOUNT_ID).setLegacyWalletId(1);
        Account account = new Account().setId(100);
        when(accountRepository.findByAccountNumber("200001")).thenReturn(List.of(account));
        service.update(accountChange);
        verify(accountRepository).delete(account);
    }
}