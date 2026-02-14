package cash.ice.api.service.impl;

import cash.ice.api.config.property.DeploymentConfigProperties;
import cash.ice.api.dto.moz.MoneyProviderMoz;
import cash.ice.api.service.TopUpServiceSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 8-1: returns allowed top-up providers per country.
 * Reads from config property ice.cash.deployment.top-up-providers-by-country,
 * falling back to all MoneyProviderMoz values for unknown countries.
 *
 * @see docs/PHASE_8_1_REGIONAL_TOPUP_SERVICE_DESIGN.md
 */
@Service
@RequiredArgsConstructor
public class DefaultTopUpServiceSelector implements TopUpServiceSelector {

    private static final List<String> ALL_PROVIDERS = Arrays.stream(MoneyProviderMoz.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    private final DeploymentConfigProperties deploymentConfig;

    @Override
    public List<String> getAllowedProviderIds(String countryCode) {
        Map<String, List<String>> byCountry = deploymentConfig.getTopUpProvidersByCountry();
        if (byCountry == null || byCountry.isEmpty()) {
            return List.copyOf(ALL_PROVIDERS);
        }
        String key = (countryCode == null || countryCode.isBlank()) ? "" : countryCode.toUpperCase();
        List<String> list = byCountry.getOrDefault(key, ALL_PROVIDERS);
        return List.copyOf(list);
    }
}
