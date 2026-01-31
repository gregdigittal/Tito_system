package cash.ice.fbc.dto.flexcube;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.utils.Tool;
import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.entity.FlexcubeAccount;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FlexcubePaymentRequest {
    private Integer externalReference;
    private String branch;
    private String product;
    private String debitAccount;
    private String debitBranch;
    private String debitValueDate;
    private String creditValueDate;
    private String creditCurrency;
    private String debitCurrency;
    private String debitAmount;
    private String paymentDetails;
    private String byOrderOf;
    private String remarks;
    private String beneficiaryAccount;
    private String beneficiaryNameAddress;
    private String beneficiaryBankBIC;
    private String user;
    private String password;

    public static FlexcubePaymentRequest create(FeesData feesData, FlexcubeAccount fbcPoolAccount, FlexcubeProperties flexcubeProperties) {
        Map<String, Object> meta = feesData.getPaymentRequest().getMeta();
        String date = Tool.currentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return new FlexcubePaymentRequest()
                .setExternalReference((Integer) meta.get("referenceId"))
                .setBranch((String) meta.get("branchCode"))
                .setProduct(flexcubeProperties.getProduct())
                .setDebitAccount(fbcPoolAccount.getDebitPoolAccount())
                .setDebitBranch(fbcPoolAccount.getDebitPoolAccountBranch())
                .setDebitValueDate(date)
                .setCreditValueDate(date)
                .setCreditCurrency(feesData.getCurrencyCode())
                .setDebitCurrency(feesData.getCurrencyCode())
                .setDebitAmount(feesData.getPaymentRequest().getAmount().toString())
                .setPaymentDetails((String) meta.get("beneficiaryReference"))
                .setByOrderOf(flexcubeProperties.getByOrderOf())
                .setRemarks(flexcubeProperties.getRemarks())
                .setBeneficiaryAccount((String) meta.get("bankAccountNo"))
                .setBeneficiaryNameAddress(meta.get("beneficiaryName") + " " + meta.get("beneficiaryAddress"))
                .setBeneficiaryBankBIC((String) meta.get("swiftCode"))
                .setUser(flexcubeProperties.getUser())
                .setPassword(flexcubeProperties.getPassword());
    }
}
