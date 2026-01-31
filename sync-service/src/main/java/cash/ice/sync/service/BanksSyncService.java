package cash.ice.sync.service;

import cash.ice.sqldb.entity.zim.Bank;
import cash.ice.sqldb.entity.zim.BankBranch;
import cash.ice.sqldb.repository.zim.BankBranchRepository;
import cash.ice.sqldb.repository.zim.BankRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BanksSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String BANKS_SQL = "select * from dbo.Banks b left join dbo.Banks_Bin_Banks bb on bb.Bank_ID = b.Bank_ID " +
            "left join dbo.Banks_BIN bin on bb.Banks_Bin_ID = bin.ID left join dbo.Banks_Flexcube bf on b.Bank_ID = bf.Bank_ID";

    private final JdbcTemplate jdbcTemplate;
    private final BankRepository bankRepository;
    private final BankBranchRepository bankBranchRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Banks");
        Map<String, Integer> banks = new HashMap<>();
        jdbcTemplate.query(BANKS_SQL, rs -> {
            String bankName = rs.getString("Bank_Name");
            Integer bankId = banks.get(bankName);
            if (bankId == null && bankName != null && !bankName.trim().isEmpty()) {
                Bank bank = bankRepository.save(new Bank()
                        .setName(bankName)
                        .setDefaultBin(rs.getString("Default_BIN")));
                banks.put(bankName, bank.getId());
                bankId = bank.getId();
            }

            String branchNo = rs.getString("Branch_No");
            int branchNoFixed = rs.getInt("Branch_No_Fixed");
            int accountId = rs.getInt("Account_ID");
            String iceAccNum = rs.getString("ICEcash_Account_Number");
            bankBranchRepository.save(new BankBranch()
                    .setBankId(bankId)
                    .setName(rs.getString("Branch_Name"))
                    .setBranchNo(branchNo != null && !branchNo.trim().isEmpty() ? Integer.parseInt(branchNo) : null)
                    .setBranchNoFixed(branchNoFixed != 0 ? branchNoFixed : null)
                    .setSwiftCode(rs.getString("Swift_Code"))
                    .setFlexcubeCode(rs.getString("Code"))
                    .setIcecashAccountNumber(iceAccNum != null && !iceAccNum.trim().isEmpty() ? iceAccNum : null)
                    .setAccountId(accountId != 0 ? accountId : null)
                    .setLegacyBankId(rs.getInt("Bank_ID"))
                    .setVisible(rs.getBoolean("Visible")));
        });
        log.info("Finished migrating Banks");
    }

    public void update(DataChange dataChange) {
        BankBranch bankBranch = bankBranchRepository.findByLegacyBankId(Integer.parseInt(dataChange.getIdentifier())).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (bankBranch != null) {
                bankBranchRepository.delete(bankBranch);
            } else {
                log.warn("Cannot delete BankBranch with code: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (bankBranch == null) {
                bankBranch = new BankBranch().setLegacyBankId(Integer.parseInt(dataChange.getIdentifier()));
            }
            fillAccountFields(bankBranch, dataChange.getData());
            bankBranchRepository.save(bankBranch);
        }
    }

    private void fillAccountFields(BankBranch bankBranch, Map<String, Object> data) {
        data.forEach((column, value) -> {
            switch (column) {
                case "BankName" -> {
                    Bank bank = bankRepository.findByName(String.valueOf(value)).orElse(null);
                    if (bank == null) {
                        bank = bankRepository.save(new Bank().setName(String.valueOf(value)));
                    }
                    bankBranch.setBankId(bank.getId());
                }
                case "BranchName" -> bankBranch.setName(getString(value));
                case "BranchNo" -> bankBranch.setBranchNo(value == null ? null : Integer.parseInt((String) value));
                case "BranchNoFixed" -> bankBranch.setBranchNoFixed(Integer.parseInt((String) value));
                case "Code" -> bankBranch.setFlexcubeCode(getString(value));
                case "SwiftCode" -> bankBranch.setSwiftCode(getString(value));
                case "ICEcashAccountNumber" -> bankBranch.setIcecashAccountNumber(getString(value));
                case "Account_ID" -> {
                    int val = Integer.parseInt((String) value);
                    bankBranch.setAccountId(val == 0 ? null : val);
                }
                case "Visible" -> bankBranch.setVisible((Boolean) value);
                default -> log.warn("Unknown BankBranch field: '{}' has value: '{}'", column, value);
            }
        });
    }

    private String getString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
