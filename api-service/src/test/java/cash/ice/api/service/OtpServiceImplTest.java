package cash.ice.api.service;

import cash.ice.api.config.property.OtpProperties;
import cash.ice.api.dto.OtpData;
import cash.ice.api.dto.OtpType;
import cash.ice.api.service.impl.OtpServiceImpl;
import cash.ice.sqldb.entity.EntityMsisdn;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {
    private static final int ENTITY_ID = 1;
    private static final int DIGITS_AMOUNT = 4;
    private static final String MSISDN = "123456789";
    private static final String COLLECTION_NAME = "collection";
    private static final String OTP_PVV = "pvv";
    private static final String OTP_KEY = "otpKey";
    private static final String OTP = "1234";

    @Mock
    private NotificationService notificationService;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private OtpProperties otpProperties;
    @InjectMocks
    private OtpServiceImpl service;

    @Test
    void testSendOtp() {
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(otpProperties.getDataCollection()).thenReturn(COLLECTION_NAME);
        when(mongoTemplate.findOne(any(Query.class), eq(OtpData.class), eq(COLLECTION_NAME))).thenReturn(null);
        when(securityPvvService.acquirePvv(any(String.class), any(String.class))).thenReturn(OTP_PVV);
        when(mongoTemplate.save(any(OtpData.class), eq(COLLECTION_NAME))).thenAnswer(invocation -> invocation.getArguments()[0]);

        OtpData actualOtpData = service.sendOtp(OtpType.MOZ_POS_LINK, ENTITY_ID, DIGITS_AMOUNT, false);
        assertThat(actualOtpData.getOtpType()).isEqualTo(OtpType.MOZ_POS_LINK);
        assertThat(actualOtpData.getMsisdn()).isEqualTo(MSISDN);
        assertThat(actualOtpData.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(actualOtpData.getOtpPvv()).isEqualTo(OTP_PVV);
        assertThat(actualOtpData.getCreatedDate()).isNotNull();
        verify(notificationService).sendSmsMessage(anyString(), eq(MSISDN));
    }

    @Test
    void testResendOtp() {
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(otpProperties.getDataCollection()).thenReturn(COLLECTION_NAME);
        when(mongoTemplate.findOne(any(Query.class), eq(OtpData.class), eq(COLLECTION_NAME))).thenReturn(
                new OtpData().setOtpKey(OTP_KEY).setOtpPvv(OTP_PVV).setCreatedDate(Instant.now().minus(1, ChronoUnit.MINUTES)));
        when(securityPvvService.restorePin(OTP_KEY, OTP_PVV, DIGITS_AMOUNT)).thenReturn(OTP);

        service.sendOtp(OtpType.MOZ_POS_LINK, ENTITY_ID, DIGITS_AMOUNT, true);
        verify(notificationService).sendSmsMessage(OTP, MSISDN);
    }

    @Test
    void testValidateOtp() {
        OtpData otpData = new OtpData().setOtpKey(OTP_KEY).setOtpPvv(OTP_PVV).setCreatedDate(Instant.now());

        when(otpProperties.getDataCollection()).thenReturn(COLLECTION_NAME);
        when(mongoTemplate.findOne(any(Query.class), eq(OtpData.class), eq(COLLECTION_NAME))).thenReturn(otpData);
        when(securityPvvService.acquirePvv(OTP_KEY, OTP)).thenReturn(OTP_PVV);
        when(otpProperties.getRequestExpirationDuration()).thenReturn(Duration.of(5, ChronoUnit.MINUTES));

        service.validateOtp(OtpType.MOZ_POS_LINK, ENTITY_ID, OTP);
        verify(mongoTemplate).remove(otpData, COLLECTION_NAME);
    }
}