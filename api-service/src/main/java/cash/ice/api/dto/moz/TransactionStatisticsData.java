package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "TransactionStatistics")
@Data
@Accessors(chain = true)
public class TransactionStatisticsData {
    @Id
    private String id;

    private int entityId;
    private String entityFirstName;
    private String entityLastName;
    private Map<String, Map<LocalDate, Stat>> transactions = new HashMap<>();       // <accountType_currency, map<day, stat>>

    public void add(String accountKey, LocalDate day, BigDecimal amount, boolean increaseCount) {
        Map<LocalDate, Stat> stats = transactions.computeIfAbsent(accountKey, k -> new HashMap<>());
        Stat stat = stats.computeIfAbsent(day, k -> new Stat());
        stat.add(amount, increaseCount);
    }

    @Data
    @Accessors(chain = true)
    public static class Stat {
        private int count;
        private BigDecimal total = BigDecimal.ZERO;

        public Stat add(BigDecimal amount, boolean increaseCount) {
            if (increaseCount) {
                count++;
            }
            total = total.add(amount);
            return this;
        }
    }
}
