package cash.ice.api.service;

import cash.ice.api.dto.moz.LinkedBankAccountMoz;

import java.util.List;

/** Phase 8-3: List, link, unlink bank accounts for entity. */
public interface LinkedBankAccountMozService {

    List<LinkedBankAccountMoz> listByEntityId(Integer entityId);

    LinkedBankAccountMoz link(Integer entityId, String bankId, String branchCode, String accountNumber, String accountName, String currency);

    boolean unlink(Integer id, Integer entityId);
}
