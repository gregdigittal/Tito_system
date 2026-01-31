package cash.ice.sync.listener;

import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sync.dto.*;
import cash.ice.sync.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headers;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataChangeListener {
    private final EntitiesSyncService entitiesSyncService;
    private final EntitiesAuthSyncService entitiesAuthSyncService;
    private final EntityTypeGroupsSyncService entityTypeGroupsSyncService;
    private final EntityTypesSyncService entityTypesSyncService;
    private final AccountsSyncService accountsSyncService;
    private final AccountRelationshipsSyncService accountRelationshipsSyncService;
    private final SecurityGroupsSyncService securityGroupsSyncService;
    private final InitiatorsSyncService initiatorsSyncService;
    private final InitiatorCategoriesSyncService initiatorCategoriesSyncService;
    private final InitiatorStatusesSyncService initiatorStatusesSyncService;
    private final ChannelsSyncService channelsSyncService;
    private final TaxDeclarationSyncService taxDeclarationSyncService;
    private final TaxReasonSyncService taxReasonSyncService;
    private final PaymentMethodSyncService paymentMethodSyncService;
    private final BanksSyncService banksSyncService;
    private final CurrenciesSyncService currenciesSyncService;
    private final AccountTypesSyncService accountTypesSyncService;
    private final DocumentTypesSyncService documentTypesSyncService;

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.entity}"})
    void listenToEntityChangeTopic(DataChange dataChange, ConsumerRecord<String, AccountChange> rec) {
        log.debug("Received EntityChange request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        EntityClass entity = entitiesSyncService.update(dataChange);
        if (entity != null) {
            entitiesAuthSyncService.update(dataChange, entity);
        }
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.account}"})
    void listenToAccountChangeTopic(AccountChange accountChange, ConsumerRecord<String, AccountChange> rec) {
        log.debug("Received Account Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        accountsSyncService.update(accountChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.account-relationship}"})
    void listenToAccountRelationshipChangeTopic(RelationshipChange relationshipChange, ConsumerRecord<String, RelationshipChange> rec) {
        log.debug("Received AccountRelationship Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        accountRelationshipsSyncService.update(relationshipChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.security-right}"})
    void listenToSecurityRightChangeTopic(SecurityRightChange dataChange, ConsumerRecord<String, SecurityRightChange> rec) {
        log.debug("Received SecurityRight Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        securityGroupsSyncService.updateRight(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.security-group}"})
    void listenToSecurityGroupChangeTopic(SecurityGroupChange dataChange, ConsumerRecord<String, SecurityGroupChange> rec) {
        log.debug("Received SecurityGroup Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        securityGroupsSyncService.updateGroup(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.initiator}"})
    void listenToInitiatorChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Initiator Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        initiatorsSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.channel}"})
    void listenToChannelChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Channel Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        channelsSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.tax-declaration}"})
    void listenToTaxDeclarationChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received TaxDeclaration Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        taxDeclarationSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.tax-reason}"})
    void listenToTaxReasonChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received TaxReason Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        taxReasonSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.payment-method}"})
    void listenToPaymentMethodChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received PaymentMethod Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        paymentMethodSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.bank}"})
    void listenToBankChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Bank Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        banksSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.currency}"})
    void listenToCurrencyChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Currency Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        currenciesSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.wallet}"})
    void listenToWalletChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Wallet Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        accountTypesSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.entity-type-group}"})
    void listenToEntityTypeGroupChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Entity Type Group Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        entityTypeGroupsSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.entity-type}"})
    void listenToEntityTypeChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Entity Type Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        entityTypesSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.initiator-category}"})
    void listenToInitiatorCategoryChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Initiator Category Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        initiatorCategoriesSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.initiator-status}"})
    void listenToInitiatorStatusChangeTopic(DataChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Initiator Status Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        initiatorStatusesSyncService.update(dataChange);
    }

    @KafkaListener(groupId = "${ice.cash.update.group}", topics = {"${ice.cash.update.topic.document-type}"})
    void listenToDocumentTypeChangeTopic(DocumentTypeChange dataChange, ConsumerRecord<String, DataChange> rec) {
        log.debug("Received Document Type Change request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        documentTypesSyncService.update(dataChange);
    }
}
