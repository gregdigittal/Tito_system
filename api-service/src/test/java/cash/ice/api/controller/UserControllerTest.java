package cash.ice.api.controller;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.controller.zim.UserRestController;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.errors.ApiExceptionHandler;
import cash.ice.api.service.*;
import cash.ice.common.utils.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static cash.ice.api.documentation.IcecashEndpointDocumentation.endpoint;
import static cash.ice.common.error.ErrorCodes.EC1012;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {UserRestController.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi", uriPort = 80)
class UserControllerTest {

    @MockBean
    private EntityRegistrationService entityRegistrationService;
    @MockBean
    private EntityService entityService;
    @MockBean
    private EntityLoginService entityLoginService;
    @MockBean
    private StaffMemberService staffMemberService;
    @MockBean
    private StaffMemberLoginService staffMemberLoginService;
    @MockBean
    private KeycloakService keycloakService;
    @MockBean
    private AuthUserService authUserService;
    @MockBean
    private StaffProperties staffProperties;
    @MockBean
    private EntitiesProperties entitiesProperties;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerUser() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/registerUserRequest.json");

        when(entityRegistrationService.registerEntity(any(RegisterEntityRequest.class))).thenReturn(
                RegisterResponse.error(EC1012, String.format("Initiator '%s' does not exist", "1234567890123456")));

        mockMvc.perform(post("/api/v1/users/register").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andDo(document("user/register", endpoint(),
                        relaxedRequestFields(
                                fieldWithPath("firstName").description("First name"),
                                fieldWithPath("lastName").description("Last name"),
                                fieldWithPath("idTypeId").description("ID Type identifier"),
                                fieldWithPath("idNumber").description("ID Number"),
                                fieldWithPath("entityType").description("Entity Type"),
                                fieldWithPath("mobile").description("Mobile number"),
                                fieldWithPath("email").description("Email"),
                                fieldWithPath("company").description("Company"),
                                fieldWithPath("card").description("Card number")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("status").description("Status of response"),
                                fieldWithPath("date").description("Date of response"),
                                fieldWithPath("errorCode").description("Error code"),
                                fieldWithPath("message").description("Response message")
                        )
                ));
    }

    @Test
    void loginUser() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/loginUserRequest.json");

        AccessTokenResponse response = new AccessTokenResponse();
        response.setTokenType("Bearer");
        response.setToken("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiUFEzYzh...");
        response.setRefreshToken("eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJj...");
        response.setExpiresIn(300);
        response.setRefreshExpiresIn(1800);
        when(entityLoginService.simpleLogin(any(LoginEntityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/login").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("user/login", endpoint(),
                        relaxedRequestFields(
                                fieldWithPath("username").description("Enter string can be Account number, Card number or ID number"),
                                fieldWithPath("password").description("PIN code password")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("token_type").description("Token type"),
                                fieldWithPath("access_token").description("Token, used in 'Authorization' request header"),
                                fieldWithPath("refresh_token").description("Refresh token"),
                                fieldWithPath("expires_in").description("Token expiration time, in seconds"),
                                fieldWithPath("refresh_expires_in").description("Refresh token expiration time, in seconds")
                        )
                ));
    }
}