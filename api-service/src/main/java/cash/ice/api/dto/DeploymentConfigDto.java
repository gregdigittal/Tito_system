package cash.ice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Phase 6: Response for GET /api/v1/config/deployment.
 * Provides country, terminology, currencies, locales, and optional UserType→AccountType mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentConfigDto {

    private String countryCode;
    private String defaultCurrencyCode;
    private String defaultLocale;
    private String vehicleTerminology;
    private String taxiOwnerLabel;
    private List<CurrencyItem> currencies;
    private List<LocaleItem> locales;
    private Map<String, String> userTypeToAccountType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyItem {
        private String isoCode;
        private boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocaleItem {
        private String languageKey;
        private String name;
    }
}
