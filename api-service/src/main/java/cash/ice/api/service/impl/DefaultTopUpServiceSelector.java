package cash.ice.api.service.impl;

import cash.ice.api.dto.moz.MoneyProviderMoz;
import cash.ice.api.service.TopUpServiceSelector;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 8-1 stub: returns allowed top-up providers per country.
 * Default mapping can be replaced by config (e.g. ice.cash.deployment.top-up-providers-by-country) later.
 *
 * @see docs/PHASE_8_1_REGIONAL_TOPUP_SERVICE_DESIGN.md
 */
@Service
public class DefaultTopUpServiceSelector implements TopUpServiceSelector {

    private static final List<String> ALL_MOZ = Arrays.stream(MoneyProviderMoz.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    /** Country code (e.g. KE, MZ) → list of allowed MoneyProviderMoz names. */
    private static final Map<String, List<String>> BY_COUNTRY = Map.of(
            "KE", Collections.singletonList(MoneyProviderMoz.MPESA.name()),
            "MZ", ALL_MOZ
    );

    @Override
    public List<String> getAllowedProviderIds(String countryCode) {
        List<String> list = (countryCode == null || countryCode.isBlank())
                ? ALL_MOZ
                : BY_COUNTRY.getOrDefault(countryCode.toUpperCase(), ALL_MOZ);
        return List.copyOf(list);
    }
}
