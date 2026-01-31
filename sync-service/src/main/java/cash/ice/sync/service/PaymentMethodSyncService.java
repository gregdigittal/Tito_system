package cash.ice.sync.service;

import cash.ice.sqldb.entity.PaymentMethod;
import cash.ice.sqldb.repository.PaymentMethodRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentMethodSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.Payment_Method";

    private final JdbcTemplate jdbcTemplate;
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating PaymentMethods");
        List<PaymentMethod> paymentMethods = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
                new PaymentMethod()
                        .setDescription(rs.getString("Description"))
                        .setFriendlyName(rs.getString("Friendly_Name")));
        paymentMethodRepository.saveAll(paymentMethods);
        log.info("Finished migrating PaymentMethods: {} processed, {} total", paymentMethods.size(), paymentMethodRepository.count());
    }

    public void update(DataChange dataChange) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByDescription(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (paymentMethod != null) {
                paymentMethodRepository.delete(paymentMethod);
            } else {
                log.warn("Cannot delete PaymentMethod with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (paymentMethod == null) {
                paymentMethod = new PaymentMethod().setDescription(dataChange.getIdentifier());
                if (dataChange.getData().containsKey("FriendlyName")) {
                    paymentMethod.setFriendlyName((String) dataChange.getData().get("FriendlyName"));
                }
                paymentMethodRepository.save(paymentMethod);
            }
        }
    }
}
