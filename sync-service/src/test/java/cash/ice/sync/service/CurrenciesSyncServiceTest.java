package cash.ice.sync.service;

import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.common.utils.Tool;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrenciesSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private CurrenciesSyncService service;

    @BeforeEach
    void init() {
        service = new CurrenciesSyncService(jdbcTemplate, currencyRepository);
    }

    @Test
    void testCurrencyCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/currencyRequest.json"), DataChange.class);
        service.update(dataChange);
        verify(currencyRepository).save(new Currency().setIsoCode("AAA").setActive(true).setPostilionCode(1));
    }

    @Test
    void testCurrencyUpdate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/currencyRequest.json"), DataChange.class);
        when(currencyRepository.findByIsoCode(dataChange.getIdentifier())).thenReturn(
                Optional.of(new Currency().setIsoCode(dataChange.getIdentifier()).setActive(false).setPostilionCode(2)));
        service.update(dataChange);
        verify(currencyRepository).save(new Currency().setIsoCode("AAA").setActive(true).setPostilionCode(1));
    }

    @Test
    void testCurrencyDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("AAA");
        Currency currency = new Currency().setIsoCode(dataChange.getIdentifier());
        when(currencyRepository.findByIsoCode(dataChange.getIdentifier())).thenReturn(Optional.of(currency));
        service.update(dataChange);
        verify(currencyRepository).delete(currency);
    }
}