package cash.ice.ledger.performance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//@Document(collection = "perfStatistics")
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PerformanceStatistics {

    @Id
    private String id;
    private String info;
    private Integer totalRecords;
    private Integer accountRecords;
//    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal min;
//    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal avg;
//    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal max;
//    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalDuration;
//    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal realDuration;
    private Integer parallelism;
    private LocalDateTime createdDate;

}
