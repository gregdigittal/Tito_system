package cash.ice.api.config.property;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 6: Deployment/country configuration. Used by the deployment config API
 * so the frontend can show correct country, terminology, currencies, and locale without code change.
 */
@Data
public class DeploymentConfigProperties {

    /** ISO country code for this deployment (e.g. KE, ZA, MZ). */
    private String countryCode = "KE";

    /** Default currency code (e.g. KES, ZAR, MZN). */
    private String defaultCurrencyCode = "KES";

    /** Default locale (e.g. en_KE, en_ZA). */
    private String defaultLocale = "en_KE";

    /** Vehicle/transport terminology label (e.g. Matatu, Minibus Taxi). */
    private String vehicleTerminology = "Matatu";

    /** User-facing label for taxi/matatu owner type. */
    private String taxiOwnerLabel = "Matatu/Taxi Owner";

    /** Optional: map frontend user type display name → backend AccountTypeMoz value. */
    private Map<String, String> userTypeToAccountType = defaultUserTypeMapping();

    private static Map<String, String> defaultUserTypeMapping() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("General User", "CommuterRegular");
        m.put("Matatu/Taxi Owner", "MatatuOwnerBusiness");
        m.put("Agents", "AgentBusiness");
        m.put("SACCO", "SaccoBusiness");
        return m;
    }
}
