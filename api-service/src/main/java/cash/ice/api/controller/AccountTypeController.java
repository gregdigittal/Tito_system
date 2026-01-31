package cash.ice.api.controller;

import cash.ice.api.dto.SortInput;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;

@Controller
@RequiredArgsConstructor
public class AccountTypeController {
    private final AccountTypeRepository accountTypeRepository;

    @MutationMapping
    public AccountType addAccountType(@Argument AccountType accountType) {
        return accountTypeRepository.save(accountType);
    }

    @MutationMapping
    public Optional<AccountType> updateAccountType(@Argument Integer id, @Argument AccountType accountType) {
        return accountTypeRepository.findById(id).map(accountType1 ->
                accountTypeRepository.save(accountType.setId(id)));
    }

    @MutationMapping
    public Optional<AccountType> deleteAccountType(@Argument Integer id) {
        Optional<AccountType> accountType = accountTypeRepository.findById(id);
        accountType.ifPresent(accountTypeRepository::delete);
        return accountType;
    }

    @QueryMapping
    public Iterable<AccountType> allAccountTypes(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return accountTypeRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    public AccountType accountTypeById(@Argument Integer id) {
        return accountTypeRepository.findById(id).orElseThrow();
    }

    @BatchMapping(typeName = "Account", field = "accountType")
    public Map<Account, AccountType> accountType(List<Account> accounts) {
        return itemsToCategoriesMap(accounts, Account::getAccountTypeId, AccountType::getId, accountTypeRepository);
    }
}
