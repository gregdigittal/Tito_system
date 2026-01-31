package cash.ice.e2e.backoffice;

import cash.ice.ApiUtil;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static cash.ice.BackofficeRequests.*;
import static cash.ice.GraphQlRequests.getCurrencies;
import static cash.ice.GraphQlRequests.getTransactionCodes;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JournalsTests {
    private static final String EMAIL = "test.backoffice.user@ice.cash";
    private static final String MOBILE = "000000000000";
    private static final String PASSWORD = "1234";
    private static final double AMOUNT = 10.0;

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer staffMemberId;
    private Integer journalId;
    private String token;

    @Test
    public void testRejectJournal() {
        try {
            var currencies = graphQL.call(getCurrencies)
                    .print("  currencies");
            int currencyId = currencies.getInt("[isoCode = ZWL].id");

            var txs = graphQL.call(String.format(getTransactionCodes, currencyId))
                    .print("  transaction codes");
            int txId = txs.getInt("content[code = PAY].id");
            String feesJson = txs.toWrapper("content[code = PAY].additionalFees")
                    .toJsonString(List.of("id", "amount"), Map.of("id", "feeId"));
            System.out.printf("  txId: %s, feesJson: %s%n", txId, feesJson);

            staffMemberId = createBackofficeUser(EMAIL, "Test backoffice user", "Backoffice user");
            token = apiUtil.otpLoginBackofficeUser(EMAIL, PASSWORD);

            var journal = graphQL.call(String.format(createJournal, currencyId, txId, AMOUNT, 4, 1, "details", "notes", feesJson, false), token)
                    .print("  journal");
            assertThat(journal.getInt("id")).isGreaterThan(0);
            assertThat(journal.getInt("transactionCodeId")).isGreaterThan(0);
            assertThat(journal.getStr("status")).isEqualTo("PENDING");
            assertThat(journal.getDbl("amount")).isEqualTo(10.0);
            assertThat(journal.getStr("drAccountId")).isEqualTo("4");
            assertThat(journal.getStr("crAccountId")).isEqualTo("1");
            journalId = journal.getInt("id");

            var reject = graphQL.call(String.format(rejectJournal, journalId), token)
                    .print("  reject");
            assertThat(reject.getStr("status")).isEqualTo("REJECTED");
        } finally {
            if (staffMemberId != null) {
                graphQL.call(String.format(deleteStaffMember, staffMemberId))
                        .print("  delete staff member");
            }
        }
    }

    @Test
    public void testAcceptJournal() {
        try {
            var currencies = graphQL.call(getCurrencies)
                    .print("  currencies");
            int currencyId = currencies.getInt("[isoCode = ZWL].id");

            var txs = graphQL.call(String.format(getTransactionCodes, currencyId))
                    .print("  transaction codes");
            int txId = txs.getInt("content[code = PAY].id");
            String feesJson = txs.toWrapper("content[code = PAY].additionalFees")
                    .toJsonString(List.of("id", "amount"), Map.of("id", "feeId"));
            System.out.printf("  txId: %s, feesJson: %s%n", txId, feesJson);

            staffMemberId = createBackofficeUser(EMAIL, "Test backoffice user", "Backoffice user");
            token = apiUtil.otpLoginBackofficeUser(EMAIL, PASSWORD);

            var journal = graphQL.call(String.format(createJournal, currencyId, txId, AMOUNT, 4, 1, "details", "notes", feesJson, false), token)
                    .print("  journal");
            journalId = journal.getInt("id");

            var accept = graphQL.call(String.format(acceptJournal, journalId), token)
                    .print("  accept");
            assertThat(accept.getStr("status")).isEqualTo("ACCEPTED");
        } finally {
            if (staffMemberId != null) {
                graphQL.call(String.format(deleteStaffMember, staffMemberId))
                        .print("  delete staff member");
            }
        }
    }

    private Integer createBackofficeUser(String email, String firstName, String lastName) {
        var staffMember = graphQL.call(String.format(createStaffMember, PASSWORD, email, firstName, lastName, MOBILE))
                .print("  backoffice user");
        return staffMember.getInt("id");
    }
}
