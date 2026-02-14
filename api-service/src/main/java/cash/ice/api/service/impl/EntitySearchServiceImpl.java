package cash.ice.api.service.impl;

import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.EntitiesSearchCriteria;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.EntitySearchService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.util.SqlUtil;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.EntityMsisdn;
import cash.ice.sqldb.entity.Initiator;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.InitiatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntitySearchServiceImpl implements EntitySearchService {
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final InitiatorRepository initiatorRepository;

    @Override
    public Page<EntityClass> searchEntities(EntitiesSearchCriteria searchBy, String searchInput, boolean exactMatch, int page, int size, SortInput sort) {
        PageRequest pageable = PageRequest.of(page, size, SortInput.toSort(sort));
        return switch (searchBy) {
            case ENTITY_ID -> searchById(searchInput, exactMatch, pageable);
            case ACCOUNT_NUMBER -> searchByAccountNumber(searchInput, exactMatch, pageable);
            case NAMES -> searchByNames(searchInput, exactMatch, pageable);
            case INITIATOR_NUMBER -> searchByInitiator(searchInput, exactMatch, pageable);
            case ID_NUMBER -> searchByIdNumber(searchInput, exactMatch, pageable);
            case MOBILE_NUMBER -> searchByMobileNumber(searchInput, exactMatch, pageable);
            default -> throw new ICEcashException("Wrong search criteria: " + searchBy, ErrorCodes.EC1047);
        };
    }

    private Page<EntityClass> searchById(String searchInput, boolean exactMatch, PageRequest pageable) {
        try {
            int index = Integer.parseInt(searchInput);
            if (exactMatch) {
                List<EntityClass> entities = entityRepository.findById(index).map(List::of).orElseGet(List::of);
                return new PageImpl<>(entities, pageable, entities.size());
            } else {
                return entityRepository.findPartialById(SqlUtil.escapeLikeParam(searchInput), pageable);
            }
        } catch (NumberFormatException e) {
            throw new ICEcashException("Wrong entityId: " + searchInput, ErrorCodes.EC1048);
        }
    }

    private EntityClass getEntityById(Integer id) {
        return entityRepository.findById(id).orElseThrow(() -> new UnexistingUserException("id: " + id));
    }

    private Page<EntityClass> searchByAccountNumber(String searchInput, boolean exactMatch, PageRequest pageable) {
        if (exactMatch) {
            List<EntityClass> accounts = accountRepository.findByAccountNumber(searchInput).stream()
                    .map(Account::getEntityId).distinct().map(this::getEntityById).toList();
            return new PageImpl<>(accounts, pageable, accounts.size());
        } else {
            Page<Account> accounts = accountRepository.findPartialByAccountNumber(searchInput, pageable);
            return new PageImpl<>(accounts.stream().map(Account::getEntityId).distinct()
                    .map(this::getEntityById).toList(), pageable, accounts.getTotalElements());
        }
    }

    private Page<EntityClass> searchByNames(String searchInput, boolean exactMatch, PageRequest pageable) {
        String[] names = searchInput.split("\\s+");
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";
        if (exactMatch) {
            return entityRepository.findByFirstNameAndLastName(firstName, lastName, pageable);
        } else {
            return entityRepository.findPartialByFirstNameAndLastName(
                    SqlUtil.escapeLikeParam(firstName), SqlUtil.escapeLikeParam(lastName), pageable);
        }
    }

    private Page<EntityClass> searchByInitiator(String searchInput, boolean exactMatch, PageRequest pageable) {
        Page<Initiator> initiators;
        if (exactMatch) {
            List<Initiator> initiatorsList = initiatorRepository.findByIdentifier(searchInput).map(List::of).orElseGet(List::of);
            initiators = new PageImpl<>(initiatorsList, pageable, initiatorsList.size());
        } else {
            initiators = initiatorRepository.findPartialByIdentifier(searchInput, pageable);
        }
        List<EntityClass> entities = initiators.stream()
                .filter(initiator -> Objects.nonNull(initiator.getAccountId()))
                .map(initiator -> accountRepository.findById(initiator.getAccountId()).orElse(null))
                .filter(Objects::nonNull).filter(account -> Objects.nonNull(account.getEntityId()))
                .map(account -> entityRepository.findById(account.getEntityId()).orElse(null))
                .filter(Objects::nonNull).toList();
        return new PageImpl<>(entities, pageable, initiators.getTotalElements());
    }

    private Page<EntityClass> searchByIdNumber(String searchInput, boolean exactMatch, PageRequest pageable) {
        if (exactMatch) {
            List<EntityClass> entities = entityRepository.findByIdNumber(searchInput);
            return new PageImpl<>(entities, pageable, entities.size());
        } else {
            return entityRepository.findPartialByIdNumber(SqlUtil.escapeLikeParam(searchInput), pageable);
        }
    }

    private Page<EntityClass> searchByMobileNumber(String searchInput, boolean exactMatch, PageRequest pageable) {
        Page<EntityMsisdn> phones;
        if (exactMatch) {
            List<EntityMsisdn> msisdnList = entityMsisdnRepository.findByMsisdn(searchInput);
            phones = new PageImpl<>(msisdnList, pageable, msisdnList.size());
        } else {
            phones = entityMsisdnRepository.findPartialByMsisdn(searchInput, pageable);
        }
        List<EntityClass> entities = phones.stream().filter(msisdn -> Objects.nonNull(msisdn.getEntityId())).map(msisdn ->
                entityRepository.findById(msisdn.getEntityId()).orElse(null)
        ).filter(Objects::nonNull).toList();
        return new PageImpl<>(entities, pageable, phones.getTotalElements());
    }
}
