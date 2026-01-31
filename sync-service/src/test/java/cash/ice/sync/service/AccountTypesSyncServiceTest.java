package cash.ice.sync.service;

import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.repository.AccountTypeRepository;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.common.utils.Tool;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTypesSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int CURRENCY_ID = 1;

    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Captor
    private ArgumentCaptor<AccountType> accountTypeCaptor;

    private AccountTypesSyncService service;

    @BeforeEach
    void init() {
        service = new AccountTypesSyncService(jdbcTemplate, currencyRepository, accountTypeRepository);
    }

    @Test
    void testAccountTypeCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/walletRequest.json"), DataChange.class);
        when(currencyRepository.findByIsoCode("ZWL")).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID)));

        service.update(dataChange);
        verify(accountTypeRepository).save(accountTypeCaptor.capture());
        AccountType actualAccountType = accountTypeCaptor.getValue();
        assertThat(actualAccountType.getLegacyWalletId()).isEqualTo("200");
        assertThat(actualAccountType.isActive()).isTrue();
        assertThat(actualAccountType.getName()).isEqualTo("Test Account Type");
        assertThat(actualAccountType.getDescription()).isEqualTo("AccountTypeDescription");
        assertThat(actualAccountType.getCurrencyId()).isEqualTo(CURRENCY_ID);
        assertThat(actualAccountType.getAuditTransactionInterval()).isEqualTo(10);
        assertThat(actualAccountType.getAuditTransactionValue()).isEqualTo(new BigDecimal("1000.0"));
    }

    @Test
    void testAccountTypeUpdate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/walletRequest.json"), DataChange.class);
        when(accountTypeRepository.findByLegacyWalletId("200")).thenReturn(
                Optional.of(new AccountType().setLegacyWalletId(dataChange.getIdentifier())));
        when(currencyRepository.findByIsoCode("ZWL")).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID)));

        service.update(dataChange);
        verify(accountTypeRepository).save(accountTypeCaptor.capture());
        AccountType actualAccountType = accountTypeCaptor.getValue();
        assertThat(actualAccountType.getLegacyWalletId()).isEqualTo("200");
        assertThat(actualAccountType.isActive()).isTrue();
        assertThat(actualAccountType.getName()).isEqualTo("Test Account Type");
        assertThat(actualAccountType.getDescription()).isEqualTo("AccountTypeDescription");
        assertThat(actualAccountType.getCurrencyId()).isEqualTo(CURRENCY_ID);
        assertThat(actualAccountType.getAuditTransactionInterval()).isEqualTo(10);
        assertThat(actualAccountType.getAuditTransactionValue()).isEqualTo(new BigDecimal("1000.0"));
    }

    @Test
    void testAccountTypeDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("200");
        AccountType accountType = new AccountType().setLegacyWalletId(dataChange.getIdentifier());
        when(accountTypeRepository.findByLegacyWalletId(dataChange.getIdentifier())).thenReturn(Optional.of(accountType));
        service.update(dataChange);
        verify(accountTypeRepository).delete(accountType);
    }
}