package cash.ice.sync.controller;

import cash.ice.sync.config.UpdateTopicProperties;
import cash.ice.sync.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/api/v1/db/update")
@RequiredArgsConstructor
@Slf4j
public class DataChangeController {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UpdateTopicProperties topicProperties;

    @PutMapping("/entity")
    @ResponseStatus(code = ACCEPTED)
    public void addEntityChange(@Valid @RequestBody DataChange request) {
        log.debug("Received entity DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getEntity(), null, request));
    }

    @PutMapping("/account")
    @ResponseStatus(code = ACCEPTED)
    public void addAccountChange(@Valid @RequestBody AccountChange request) {
        log.debug("Received account DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getAccount(), null, request));
    }

    @PutMapping("/account-relationship")
    @ResponseStatus(code = ACCEPTED)
    public void addAccountRelationshipChange(@Valid @RequestBody RelationshipChange request) {
        log.debug("Received accountRelationship DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getAccountRelationship(), null, request));
    }

    @PutMapping("/security-right")
    @ResponseStatus(code = ACCEPTED)
    public void addSecurityRightChange(@Valid @RequestBody SecurityRightChange request) {
        log.debug("Received security right DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getSecurityRight(), null, request));
    }

    @PutMapping("/security-group")
    @ResponseStatus(code = ACCEPTED)
    public void addSecurityGroupChange(@Valid @RequestBody SecurityGroupChange request) {
        log.debug("Received security group DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getSecurityGroup(), null, request));
    }

    @PutMapping("/initiator")
    @ResponseStatus(code = ACCEPTED)
    public void addInitiatorChange(@Valid @RequestBody DataChange request) {
        log.debug("Received initiator DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getInitiator(), null, request));
    }

    @PutMapping("/channel")
    @ResponseStatus(code = ACCEPTED)
    public void addChannelChange(@Valid @RequestBody DataChange request) {
        log.debug("Received channel DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getChannel(), null, request));
    }

    @PutMapping("/tax-declaration")
    @ResponseStatus(code = ACCEPTED)
    public void addTaxDeclarationChange(@Valid @RequestBody DataChange request) {
        log.debug("Received channel DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getTaxDeclaration(), null, request));
    }

    @PutMapping("/tax-reason")
    @ResponseStatus(code = ACCEPTED)
    public void addTaxReasonChange(@Valid @RequestBody DataChange request) {
        log.debug("Received channel DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getTaxReason(), null, request));
    }

    @PutMapping("/payment-method")
    @ResponseStatus(code = ACCEPTED)
    public void addPaymentMethodChange(@Valid @RequestBody DataChange request) {
        log.debug("Received channel DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getPaymentMethod(), null, request));
    }

    @PutMapping("/bank")
    @ResponseStatus(code = ACCEPTED)
    public void addBankChange(@Valid @RequestBody DataChange request) {
        log.debug("Received bank DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getBank(), null, request));
    }

    @PutMapping("/currency")
    @ResponseStatus(code = ACCEPTED)
    public void addCurrencyChange(@Valid @RequestBody DataChange request) {
        log.debug("Received currency DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getCurrency(), null, request));
    }

    @PutMapping("/wallet")
    @ResponseStatus(code = ACCEPTED)
    public void addWalletChange(@Valid @RequestBody DataChange request) {
        log.debug("Received wallet DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getWallet(), null, request));
    }

    @PutMapping("/entity-type-group")
    @ResponseStatus(code = ACCEPTED)
    public void addEntityTypeGroupChange(@Valid @RequestBody DataChange request) {
        log.debug("Received entity type group DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getEntityTypeGroup(), null, request));
    }

    @PutMapping("/entity-type")
    @ResponseStatus(code = ACCEPTED)
    public void addEntityTypeChange(@Valid @RequestBody DataChange request) {
        log.debug("Received entity type DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getEntityType(), null, request));
    }

    @PutMapping("/initiator-category")
    @ResponseStatus(code = ACCEPTED)
    public void addInitiatorCategoryChange(@Valid @RequestBody DataChange request) {
        log.debug("Received initiator category DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getInitiatorCategory(), null, request));
    }

    @PutMapping("/initiator-status")
    @ResponseStatus(code = ACCEPTED)
    public void addInitiatorStatusChange(@Valid @RequestBody DataChange request) {
        log.debug("Received initiator status DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getInitiatorStatus(), null, request));
    }

    @PutMapping("/document-type")
    @ResponseStatus(code = ACCEPTED)
    public void addDocumentTypeChange(@Valid @RequestBody DocumentTypeChange request) {
        log.debug("Received document type DB update request: " + request);
        kafkaTemplate.send(new ProducerRecord<>(topicProperties.getDocumentType(), null, request));
    }
}
