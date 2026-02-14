package cash.ice.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies security configuration: public endpoints are accessible without auth,
 * protected endpoints require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ApiSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void configDeployment_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/config/deployment"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void paymentsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/payments/pending/some-id"))
                .andExpect(status().isUnauthorized());
    }
}
