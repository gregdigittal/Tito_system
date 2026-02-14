package cash.ice.api.service.impl;

import cash.ice.api.dto.moz.LinkedBankAccountMoz;
import cash.ice.api.service.LinkedBankAccountMozService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.LinkedBankAccount;
import cash.ice.sqldb.entity.LinkedBankAccountStatus;
import cash.ice.sqldb.repository.LinkedBankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedBankAccountMozServiceImpl implements LinkedBankAccountMozService {

    private final LinkedBankAccountRepository linkedBankAccountRepository;

    @Override
    public List<LinkedBankAccountMoz> listByEntityId(Integer entityId) {
        return linkedBankAccountRepository.findByEntityIdOrderByCreatedDateDesc(entityId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(timeout = 30)
    public LinkedBankAccountMoz link(Integer entityId, String bankId, String branchCode, String accountNumber, String accountName, String currency) {
        LinkedBankAccount entity = new LinkedBankAccount()
                .setEntityId(entityId)
                .setBankId(bankId)
                .setBranchCode(branchCode)
                .setAccountNumber(accountNumber)
                .setAccountName(accountName != null ? accountName : "")
                .setCurrency(currency != null ? currency : "MZN")
                .setStatus(LinkedBankAccountStatus.PENDING_VERIFICATION)
                .setCreatedDate(LocalDateTime.now());
        entity = linkedBankAccountRepository.save(entity);
        log.info("AUDIT: Bank account LINKED — entityId={}, bankId={}, accountNumber={}, linkedBankAccountId={}",
                entityId, bankId, accountNumber, entity.getId());
        return toDto(entity);
    }

    @Override
    @Transactional(timeout = 30)
    public boolean unlink(Integer id, Integer entityId) {
        return linkedBankAccountRepository.findByIdAndEntityId(id, entityId)
                .map(acc -> {
                    linkedBankAccountRepository.delete(acc);
                    log.info("AUDIT: Bank account UNLINKED — entityId={}, bankId={}, accountNumber={}, linkedBankAccountId={}",
                            entityId, acc.getBankId(), acc.getAccountNumber(), id);
                    return true;
                })
                .orElseThrow(() -> new ICEcashException("Linked bank account not found or access denied", ErrorCodes.EC1022));
    }

    private LinkedBankAccountMoz toDto(LinkedBankAccount e) {
        return new LinkedBankAccountMoz()
                .setId(e.getId())
                .setBankId(e.getBankId())
                .setBranchCode(e.getBranchCode())
                .setAccountNumber(e.getAccountNumber())
                .setAccountName(e.getAccountName())
                .setCurrency(e.getCurrency())
                .setStatus(e.getStatus() != null ? e.getStatus().name() : null)
                .setCreatedAt(e.getCreatedDate());
    }
}
