package cash.ice.api.util;

import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class MappingUtil {

    private MappingUtil() {
    }

    public static <I, C> Map<I, C> itemsToCategoriesMap(List<I> items, Function<I, Integer> iCategoryIdFunc,
                                                        Function<C, Integer> cIdFunc, JpaRepository<C, Integer> categoryRepo) {
        List<Integer> categoryIds = items.stream().map(iCategoryIdFunc).distinct().toList();
        Map<Integer, C> categoriesMap = categoryRepo.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(cIdFunc, item -> item));
        HashMap<I, C> resultMap = new HashMap<>();
        items.forEach(item -> resultMap.put(item, categoriesMap.get(iCategoryIdFunc.apply(item))));
        return resultMap;
    }

    public static <I, C, V> Map<I, V> itemsToCategoriesMap(List<I> items, Function<I, Integer> iCategoryIdFunc,
                                                           Function<C, Integer> cIdFunc, JpaRepository<C, Integer> categoryRepo, Function<C, V> valueExtractor) {
        List<Integer> categoryIds = items.stream().map(iCategoryIdFunc).distinct().toList();
        Map<Integer, C> categoriesMap = categoryRepo.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(cIdFunc, item -> item));
        HashMap<I, V> resultMap = new HashMap<>();
        items.forEach(item -> {
            C category = categoriesMap.get(iCategoryIdFunc.apply(item));
            resultMap.put(item, category != null ? valueExtractor.apply(category) : null);
        });
        return resultMap;
    }

    public static <C, I> Map<C, List<I>> categoriesToItemsListMap(List<C> categories, Function<C, Integer> cIdFunc,
                                                                  Function<I, Integer> iCategoryIdFunc,
                                                                  Function<List<Integer>, List<I>> extractItemsFunc) {
        List<Integer> categoryIds = categories.stream().map(cIdFunc).toList();
        List<I> items = extractItemsFunc.apply(categoryIds);
        Map<Integer, C> cMap = categories.stream().collect(Collectors.toMap(cIdFunc, e -> e));
        Map<Integer, List<I>> result = items.stream().collect(Collectors.groupingBy(iCategoryIdFunc));
        return result.entrySet().stream().collect(Collectors.toMap(e -> cMap.get(e.getKey()), Map.Entry::getValue));
    }

    public static Map<Account, BigDecimal> accountBalancesMap(List<Account> accounts, Function<List<Integer>,
            List<AccountBalance>> extractAccountBalancesFunc, Supplier<BigDecimal> defaultValue) {
        Map<Account, List<AccountBalance>> accountBalanceMap = categoriesToItemsListMap(accounts, Account::getId,
                AccountBalance::getAccountId, extractAccountBalancesFunc);
        accounts.forEach(account -> accountBalanceMap.putIfAbsent(account, List.of()));
        Map<Account, BigDecimal> result = new HashMap<>();
        accountBalanceMap.forEach((account, accountBalances) -> result.put(account,
                accountBalances.stream().map(AccountBalance::getBalance).findAny().orElse(defaultValue.get())));
        return result;
    }
}
