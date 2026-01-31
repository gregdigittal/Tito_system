package cash.ice.sync.controller;

import cash.ice.common.utils.Tool;
import cash.ice.sync.config.UpdateTopicProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DataChangeController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {DataChangeController.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi/sync-service", uriPort = 80)
class DataChangeControllerTest {

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;
    @MockBean
    private UpdateTopicProperties topicProperties;
    @Captor
    private ArgumentCaptor<ProducerRecord<String, Object>> recordArgumentCaptor;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void updateEntity() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/entityRequest.json");
        when(topicProperties.getEntity()).thenReturn("updateEntityTopic");

        mockMvc.perform(put("/api/v1/db/update/entity").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/entity-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("Legacy `Account_ID` value, required"),
                                fieldWithPath("data").description("Update data, `dbo.Accounts` column names are keys")
                        )
                ));
    }

    @Test
    void updateAccount() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/accountRequest.json");
        when(topicProperties.getAccount()).thenReturn("updateAccountTopic");

        mockMvc.perform(put("/api/v1/db/update/account").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/account-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("legacyAccountId").description("Legacy `Account_ID` value, required"),
                                fieldWithPath("legacyWalletId").description("Legacy `Wallet_ID` value, required"),
                                fieldWithPath("data").description("Update data, `dbo.Accounts_Profile_Transactions` column names are keys"),
                                fieldWithPath("data.Active").description("`Active` column of `dbo.Account` table"),
                                fieldWithPath("data.Daily_Limit").description("`Daily_Limit` column of `dbo.Account` table"),
                                fieldWithPath("data.Created_Date").description("`Created_Date` column of `dbo.Account` table")
                        )
                ));
    }

    @Test
    void updateAccountRelationship() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/accountRelationship.json");
        when(topicProperties.getAccountRelationship()).thenReturn("updateAccountRelationshipTopic");

        mockMvc.perform(put("/api/v1/db/update/account-relationship").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/account-relationship-request-example",
                        requestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("legacyAccountId").description("Legacy `Account_ID` value, required"),
                                fieldWithPath("legacyPartnerId").description("Legacy `Partner_ID` value, required"),
                                fieldWithPath("data.SecurityGroupOnline").description("Security group ID for 'online' realm"),
                                fieldWithPath("data.SecurityGroupMobi").description("Security group ID for 'mobi' realm"),
                                fieldWithPath("data.Other_Info1").description("Other info"),
                                fieldWithPath("data.Other_Info2").description("Other info"),
                                fieldWithPath("data.Other_Info3").description("Other info"),
                                fieldWithPath("data.Other_Info4").description("Other info"),
                                fieldWithPath("data.Other_Info5").description("Other info"),
                                fieldWithPath("data.Other_Info6").description("Other info"),
                                fieldWithPath("data.Other_Info7").description("Other info"),
                                fieldWithPath("data.Other_Info8").description("Other info"),
                                fieldWithPath("data.Other_Info9").description("Other info"),
                                fieldWithPath("data.Other_Info10").description("Other info"),
                                fieldWithPath("data.Other_Info11").description("Other info"),
                                fieldWithPath("data.Other_Info12").description("Other info"),
                                fieldWithPath("data.Other_Info13").description("Other info"),
                                fieldWithPath("data.Other_Info14").description("Other info"),
                                fieldWithPath("data.Other_Info15").description("Other info"),
                                fieldWithPath("data.Other_Info16").description("Other info"),
                                fieldWithPath("data.Other_Info17").description("Other info"),
                                fieldWithPath("data.Other_Info18").description("Other info"),
                                fieldWithPath("data.Other_Info19").description("Other info"),
                                fieldWithPath("data.Other_Info20").description("Other info")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getAccountRelationship());
    }

    @Test
    void updateSecurityRight() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/securityRight.json");
        when(topicProperties.getSecurityRight()).thenReturn("updateSecurityRightTopic");

        mockMvc.perform(put("/api/v1/db/update/security-right").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/security-right-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("right").description("Right name value, required"),
                                fieldWithPath("data.Description").description("Description of the right"),
                                fieldWithPath("data.RightType").description("Right type if need")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getSecurityRight());
    }

    @Test
    void updateSecurityGroup() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/securityGroup.json");
        when(topicProperties.getSecurityGroup()).thenReturn("updateSecurityGroupTopic");

        mockMvc.perform(put("/api/v1/db/update/security-group").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/security-group-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("Legacy group ID value, required"),
                                fieldWithPath("type").description("Group type, usually either 'online' or 'mobi', required"),
                                fieldWithPath("data.Name").description("Name of the group"),
                                fieldWithPath("data.Description").description("Description of the group"),
                                fieldWithPath("data.Active").description("Whether group is Active of not"),
                                fieldWithPath("data.Rights").description("Map of right and theirs action"),
                                fieldWithPath("data.Rights.SOME_ROLE").description("Key is role name eg SOME_ROLE, Value is either ADD or DELETE")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getSecurityGroup());
    }

    @Test
    void updateInitiator() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/initiatorRequest.json");
        when(topicProperties.getInitiator()).thenReturn("updateInitiatorTopic");

        mockMvc.perform(put("/api/v1/db/update/initiator").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/initiator-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("Legacy `Account_ID` value, required"),
                                fieldWithPath("data").description("Update data, `dbo.Cards` column names are keys")
                        )
                ));
    }

    @Test
    void updateChannel() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/channelRequest.json");
        when(topicProperties.getChannel()).thenReturn("updateChannelTopic");

        mockMvc.perform(put("/api/v1/db/update/channel").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/channel-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Channel` column value of `dbo.Channels` table, required"),
                                fieldWithPath("data").description("Update data, `dbo.Channels` column names are keys")
                        )
                ));
    }

    @Test
    void updateTaxDeclaration() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/taxDeclarationRequest.json");
        when(topicProperties.getTaxDeclaration()).thenReturn("updateTaxDeclarationTopic");

        mockMvc.perform(put("/api/v1/db/update/tax-declaration").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/tax-declaration-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("Name of tax declaration, required")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getTaxDeclaration());
    }

    @Test
    void updateTaxReason() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/taxReasonRequest.json");
        when(topicProperties.getTaxReason()).thenReturn("updateTaxReasonTopic");

        mockMvc.perform(put("/api/v1/db/update/tax-reason").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/tax-reason-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("Name of tax reason, required"),
                                fieldWithPath("data.Display").description("Whether to display of not")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getTaxReason());
    }

    @Test
    void updatePaymentMethod() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/paymentMethodRequest.json");
        when(topicProperties.getPaymentMethod()).thenReturn("updatePaymentMethodTopic");

        mockMvc.perform(put("/api/v1/db/update/payment-method").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/payment-method-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("Name of payment method, required"),
                                fieldWithPath("data.FriendlyName").description("Friendly name of payment method")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getPaymentMethod());
    }

    @Test
    void updateBank() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/bankRequest.json");
        when(topicProperties.getBank()).thenReturn("updateBankTopic");

        mockMvc.perform(put("/api/v1/db/update/bank").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/bank-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("ID value of `dbo.Banks` table, required"),
                                fieldWithPath("data.BankName").description("Name of the bank"),
                                fieldWithPath("data.BranchName").description("Name of the bank branch"),
                                fieldWithPath("data.BranchNo").description("Bank branch number"),
                                fieldWithPath("data.BranchNoFixed").description("Bank branch fixed number"),
                                fieldWithPath("data.Code").description("Bank branch code"),
                                fieldWithPath("data.SwiftCode").description("Bank swift code"),
                                fieldWithPath("data.ICEcashAccountNumber").description("ICEcash account number of Bank"),
                                fieldWithPath("data.Account_ID").description("Legacy Account ID of the bank"),
                                fieldWithPath("data.Visible").description("Whether the Bank is visible or not")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getBank());
    }

    @Test
    void updateCurrency() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/currencyRequest.json");
        when(topicProperties.getCurrency()).thenReturn("updateCurrencyTopic");

        mockMvc.perform(put("/api/v1/db/update/currency").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/currency-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Currency` column value of `dbo.Currency` table, required"),
                                fieldWithPath("data").description("Update data, `dbo.Currency` column names are keys")
                        )
                ));
    }

    @Test
    void updateWallet() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/walletRequest.json");
        when(topicProperties.getWallet()).thenReturn("updateWalletTopic");

        mockMvc.perform(put("/api/v1/db/update/wallet").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/wallet-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Wallet_ID` column value of `dbo.Wallets` table, required"),
                                fieldWithPath("data").description("Update data, `dbo.Wallets` column names are keys")
                        )
                ));
    }

    @Test
    void updateEntityTypeGroup() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/entityTypeGroupRequest.json");
        when(topicProperties.getEntityTypeGroup()).thenReturn("updateEntityTypeGroupTopic");

        mockMvc.perform(put("/api/v1/db/update/entity-type-group").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/entity-type-group-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Account_Type_Group` column value of `dbo.Accounts_Types_Groups` table, required")
                        )
                ));
    }

    @Test
    void updateEntityType() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/entityTypeRequest.json");
        when(topicProperties.getEntityType()).thenReturn("updateEntityTypeTopic");

        mockMvc.perform(put("/api/v1/db/update/entity-type").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/entity-type-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Account_Type` column value of `dbo.Accounts_Types` table, required"),
                                fieldWithPath("data").description("Update data, `dbo.Accounts_Types` column names are keys")
                        )
                ));
    }

    @Test
    void updateInitiatorCategory() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/initiatorCategoryRequest.json");
        when(topicProperties.getInitiatorCategory()).thenReturn("updateInitiatorCategoryTopic");

        mockMvc.perform(put("/api/v1/db/update/initiator-category").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/initiator-category-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Category` column value of `dbo.Card_Category` table, required"),
                                fieldWithPath("data").description("Update data, `dbo.Card_Category` column names are keys")
                        )
                ));
    }

    @Test
    void updateInitiatorStatus() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/initiatorStatusRequest.json");
        when(topicProperties.getInitiatorStatus()).thenReturn("updateInitiatorStatusTopic");

        mockMvc.perform(put("/api/v1/db/update/initiator-status").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/initiator-status-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("identifier").description("`Status_Name` column value of `dbo.Cards_Status` table, required"),
                                fieldWithPath("data").description("Update data, `dbo.Cards_Status` column names are keys")
                        )
                ));
    }

    @Test
    void updateDocumentType() throws Exception {
        String jsonRequest = Tool.readResourceAsString("update/json/documentType.json");
        when(topicProperties.getDocumentType()).thenReturn("updateDocumentTypeTopic");

        mockMvc.perform(put("/api/v1/db/update/document-type").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("data-update/document-type-request-example",
                        relaxedRequestFields(
                                fieldWithPath("action").description("Change action type, either UPDATE or DELETE, required"),
                                fieldWithPath("documentTypeId").description("Document type ID, required"),
                                fieldWithPath("accountTypeId").description("Account type ID, required"),
                                fieldWithPath("data.DocumentType").description("Document type"),
                                fieldWithPath("data.Required").description("Whether document type is required or not")
                        )
                ));
        verify(kafkaTemplate).send(recordArgumentCaptor.capture());
        assertThat(recordArgumentCaptor.getValue().topic()).isEqualTo(topicProperties.getDocumentType());
    }
}