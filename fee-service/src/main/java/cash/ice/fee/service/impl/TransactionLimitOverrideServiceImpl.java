package cash.ice.fee.service.impl;

import cash.ice.fee.service.TransactionLimitOverrideService;
import cash.ice.sqldb.entity.TransactionLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitOverrideServiceImpl implements TransactionLimitOverrideService {

    @Override
    public List<TransactionLimit> overrideLimits(List<TransactionLimit> limits) {
        if (limits.size() > 1) {
            limits = new ArrayList<>(limits);
            int[] bitMaps = limits.stream().mapToInt(limit ->
                    (nonNull(limit.getTransactionCodeId()) ? 64 : 0) +
                            (nonNull(limit.getKycStatusId()) ? 32 : 0) +
                            (nonNull(limit.getEntityTypeId()) ? 16 : 0) +
                            (nonNull(limit.getAccountTypeId()) ? 8 : 0) +
                            (nonNull(limit.getInitiatorTypeId()) ? 4 : 0) +
                            (nonNull(limit.getTier()) ? 2 : 0) +
                            (nonNull(limit.getAuthorisationType()) ? 1 : 0)
            ).toArray();
            overrideLimits(limits, bitMaps);
            limits = limits.stream().filter(Objects::nonNull).toList();
        }
        return limits;
    }

    private void overrideLimits(List<TransactionLimit> limits, int[] bitMaps) {
        log.debug("    binary map: {} for data: {}", toBinaryString(bitMaps), Arrays.toString(bitMaps));
        for (int i = 0; i < bitMaps.length - 1; i++) {
            if (limits.get(i) != null) {
                for (int j = i + 1; j < bitMaps.length; j++) {
                    if (limits.get(j) != null) {
                        if (isOverride(bitMaps, i, j)) {
                            log.debug("   .({}) {} overrides {} ({})", toBinaryString(bitMaps[j]), j, i, toBinaryString(bitMaps[i]));
                            limits.set(i, null);
                            break;
                        } else if (isOverride(bitMaps, j, i)) {
                            log.debug("    ({}) {} overrides {} ({})", toBinaryString(bitMaps[i]), i, j, toBinaryString(bitMaps[j]));
                            limits.set(j, null);
                        }
                    }
                }
            }
        }
        log.debug("    final limits: {}", limits.stream().map(limit -> limit == null ? "0" : limit.getId()).toList());
    }

    private boolean isOverride(int[] scores, int a, int b) {        // if A & (A ^ B) == 0 and B & (A ^ B) != 0 then B overrides A
        return (scores[a] & (scores[a] ^ scores[b])) == 0;
    }

    private String toBinaryString(int[] array) {
        return Arrays.stream(array).mapToObj(this::toBinaryString).toList().toString();
    }

    private String toBinaryString(int value) {
        return String.format("%7s", Integer.toBinaryString(value)).replace(' ', '0');
    }
}
