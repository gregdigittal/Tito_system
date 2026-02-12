package cash.ice.api.service;

import java.util.List;

/**
 * Phase 8-1: Returns which top-up provider(s) are available for a given country.
 * Used to validate provider choice and (when exposed via config API) to drive UI options.
 *
 * @see cash.ice.api.dto.moz.MoneyProviderMoz
 * @see docs/PHASE_8_1_REGIONAL_TOPUP_SERVICE_DESIGN.md
 */
public interface TopUpServiceSelector {

    /**
     * Returns the list of allowed top-up provider identifiers for the given country code.
     * Values should match {@link cash.ice.api.dto.moz.MoneyProviderMoz} names (e.g. MPESA, EMOLA).
     *
     * @param countryCode ISO country code (e.g. KE, MZ)
     * @return non-null list of provider ids allowed for that country; empty if none configured
     */
    List<String> getAllowedProviderIds(String countryCode);
}
