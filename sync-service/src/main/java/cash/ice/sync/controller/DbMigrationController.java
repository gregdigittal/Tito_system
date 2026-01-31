package cash.ice.sync.controller;

import cash.ice.sync.dto.DbMigrationRequest;
import cash.ice.sync.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/db/sync")
@RequiredArgsConstructor
@Slf4j
public class DbMigrationController {
    private static final String CHANNELS = "Channels";
    private static final String TAX_DECLARATION = "TaxDeclaration";
    private static final String TAX_REASON = "TaxReason";
    private static final String PAYMENT_METHOD = "PaymentMethod";
    private static final String BANK = "Bank";
    private static final String CURRENCIES = "Currencies";
    private static final String COUNTRIES = "Countries";
    private static final String ACCOUNT_TYPES = "AccountTypes";
    private static final String ENTITIES = "Entities";
    private static final String ACCOUNTS = "Accounts";
    private static final String ACCOUNT_RELATIONSHIPS = "AccountRelationships";
    private static final String SECURITY_GROUPS = "SecurityGroups";
    private static final String INITIATORS = "Initiators";

    private final ChannelsSyncService channelsSyncService;
    private final TaxDeclarationSyncService taxDeclarationSyncService;
    private final TaxReasonSyncService taxReasonSyncService;
    private final PaymentMethodSyncService paymentMethodSyncService;
    private final BanksSyncService banksSyncService;
    private final CurrenciesSyncService currenciesSyncService;
    private final CountriesSyncService countriesSyncService;
    private final AccountTypesSyncService accountTypesSyncService;
    private final EntitiesSyncService entitiesSyncService;
    private final AccountsSyncService accountsSyncService;
    private final AccountRelationshipsSyncService accountRelationshipsSyncService;
    private final SecurityGroupsSyncService securityGroupsSyncService;
    private final InitiatorsSyncService initiatorsSyncService;

    @PostMapping
    public ResponseEntity<Object> testMongoLedgerInit(@Valid @RequestBody DbMigrationRequest request) {
        List<String> services = request.getMigrateServices();
        if (request.getMigrateAll()) {
            services = List.of(CHANNELS, TAX_DECLARATION, TAX_REASON, PAYMENT_METHOD, BANK, CURRENCIES, COUNTRIES,
                    ACCOUNT_TYPES, ENTITIES, ACCOUNTS, ACCOUNT_RELATIONSHIPS, SECURITY_GROUPS, INITIATORS);
        } else if (services == null) {
            services = Collections.emptyList();
        }
        log.debug("Received DB sync request: " + request);

        List<String> performedServices = new ArrayList<>();
        if (services.contains(CHANNELS)) {
            channelsSyncService.migrateData();
            performedServices.add(CHANNELS);
        }
        if (services.contains(TAX_DECLARATION)) {
            taxDeclarationSyncService.migrateData();
            performedServices.add(TAX_DECLARATION);
        }
        if (services.contains(TAX_REASON)) {
            taxReasonSyncService.migrateData();
            performedServices.add(TAX_REASON);
        }
        if (services.contains(PAYMENT_METHOD)) {
            paymentMethodSyncService.migrateData();
            performedServices.add(PAYMENT_METHOD);
        }
        if (services.contains(BANK)) {
            banksSyncService.migrateData();
            performedServices.add(BANK);
        }
        if (services.contains(CURRENCIES)) {
            currenciesSyncService.migrateData();
            performedServices.add(CURRENCIES);
        }
        if (services.contains(COUNTRIES)) {
            countriesSyncService.migrateData();
            performedServices.add(COUNTRIES);
        }
        if (services.contains(ACCOUNT_TYPES)) {
            accountTypesSyncService.migrateData();
            performedServices.add(ACCOUNT_TYPES);
        }
        if (services.contains(ENTITIES)) {
            entitiesSyncService.migrateData();
            performedServices.add(ENTITIES);
        }
        if (services.contains(ACCOUNTS)) {
            accountsSyncService.migrateData();
            performedServices.add(ACCOUNTS);
        }
        if (services.contains(ACCOUNT_RELATIONSHIPS)) {
            accountRelationshipsSyncService.migrateData();
            performedServices.add(ACCOUNT_RELATIONSHIPS);
        }
        if (services.contains(SECURITY_GROUPS)) {
            securityGroupsSyncService.migrateData();
            performedServices.add(SECURITY_GROUPS);
        }
        if (services.contains(INITIATORS)) {
            initiatorsSyncService.migrateData();
            performedServices.add(INITIATORS);
        }
        String servicesStr = request.getMigrateAll() ? "All" : performedServices.toString();
        return new ResponseEntity<>(servicesStr + " services performed", HttpStatus.OK);
    }
}
