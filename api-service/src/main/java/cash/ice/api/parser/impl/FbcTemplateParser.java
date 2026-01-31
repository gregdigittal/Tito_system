package cash.ice.api.parser.impl;

import cash.ice.api.entity.zim.Payment;
import cash.ice.api.errors.BulkPaymentParseException;
import cash.ice.api.parser.PaymentsBulkParser;
import cash.ice.sqldb.entity.PaymentLine;
import cash.ice.sqldb.entity.zim.Bank;
import cash.ice.sqldb.entity.zim.BankBranch;
import cash.ice.sqldb.repository.zim.BankBranchRepository;
import cash.ice.sqldb.repository.zim.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FbcTemplateParser implements PaymentsBulkParser {
    public static final String RTGS_TEMPLATE = "RtgsPayment";
    private static final String DEFAULT_CURRENCY = "ZWL";
    private static final Map<String, String> PAYMENT_METHODS =
            Map.of("ZIPIT", "", "RTGS", "TRN");     // todo transactionCode for ZIPIT

    private final BankRepository bankRepository;
    private final BankBranchRepository bankBranchRepository;

    @Override
    public List<PaymentLine> parseExcelStream(InputStream inputStream, Payment payment) {
        List<PaymentLine> paymentLines = new ArrayList<>();
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 4; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (!isRowEmpty(row)) {
                    validateRow(row, i);
                    Bank bank = getBank(row.getCell(2).getStringCellValue(), i);
                    BankBranch bankBranch = getBankBranch(bank, row.getCell(3).getStringCellValue(), i);
                    PaymentLine paymentLine = new PaymentLine()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setTransactionCode(PAYMENT_METHODS.get(row.getCell(4).getStringCellValue()))
                            .setAmount(new BigDecimal(row.getCell(6).getStringCellValue()))
                            .setDetails(payment.getDescription())
                            .setMeta(Map.of(
                                    "beneficiaryName", row.getCell(0).getStringCellValue(),
                                    "beneficiaryReference", row.getCell(1).getStringCellValue(),
                                    "bankName", row.getCell(2).getStringCellValue(),
                                    "bankBin", bank.getDefaultBin(),
                                    "branchCode", bankBranch.getFlexcubeCode() != null ?
                                            bankBranch.getFlexcubeCode() : String.valueOf(bankBranch.getBranchNo()),
                                    "swiftCode", bankBranch.getSwiftCode(),
                                    "paymentMethod", row.getCell(4).getStringCellValue(),
                                    "bankAccountNo", row.getCell(5).getStringCellValue()
                            ));
                    paymentLines.add(paymentLine);
                }
            }
        } catch (IOException e) {
            throw new BulkPaymentParseException(e.getMessage());
        }
        return paymentLines;
    }

    private BankBranch getBankBranch(Bank bank, String branchStr, int index) {
        int ind = branchStr.lastIndexOf("(");
        String branchName = ind > -1 ? branchStr.substring(0, ind).trim() : branchStr.trim();
        String[] strings = StringUtils.substringsBetween(branchStr, "(", ")");
        try {
            if (strings != null && strings.length > 0) {
                int branchNo = Integer.parseInt(strings[strings.length - 1].trim());
                return bankBranchRepository.findByBankIdAndBranchNoAndName(bank.getId(), branchNo, branchName)
                        .orElseThrow(() -> new BulkPaymentParseException(index + 1, 3, "Wrong branch: " + branchStr));
            }
        } catch (NumberFormatException ignored) {
        }
        return bankBranchRepository.findByBankIdAndName(bank.getId(), branchName)
                .orElseThrow(() -> new BulkPaymentParseException(index + 1, 3, "Wrong branch: " + branchStr));
    }

    private Bank getBank(String bankName, int index) {
        return bankRepository.findByName(bankName).orElseThrow(() ->
                new BulkPaymentParseException(index + 1, 2, "Wrong bank name: " + bankName));
    }

    private void validateRow(Row row, int index) {
        for (int i = 0; i < 7; i++) {
            Cell cell = row.getCell(i);
            if (isCellEmpty(cell)) {
                throw new BulkPaymentParseException(index + 1, i, "Cell must not be empty");
            } else if (cell.getCellType() != CellType.STRING) {
                throw new BulkPaymentParseException(index + 1, i, "Cell has wrong format, must be STRING");
            }
        }
        if (!PAYMENT_METHODS.containsKey(row.getCell(4).getStringCellValue())) {
            throw new BulkPaymentParseException(index + 1, 4, "Unknown value");
        }
        try {
            new BigDecimal(row.getCell(6).getStringCellValue());
        } catch (Exception e) {
            throw new BulkPaymentParseException(index + 1, 6, "Wrong amount");
        }
    }

    public static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }

    public static boolean isCellEmpty(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK ||
                cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty();
    }
}
