package cash.ice.api.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityPvvServiceImplTest {
    private static final String CARD = "6087300000010337";
    private static final String PIN = "1234";
    private static final String PAN = "041247FFFFFFEFCC";
    private static final String PVV = "FD9CC6A7E7414222";

    private SecurityPvvServiceImpl service;

    @BeforeEach
    void init() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        service = new SecurityPvvServiceImpl();
        service.hexCipherKey = "19680607195506111957020119810623";
        service.init();
    }

    @Test
    void acquirePvv() {
        String actualPvv = service.acquirePvv(CARD, PIN);
        assertThat(actualPvv).isEqualTo(PVV);
    }

    @Test
    void restorePan() {
        String actualPan = service.restorePan(PVV);
        assertThat(actualPan).isEqualTo(PAN);
    }

    @Test
    void restorePin() {
        String actualPin = service.restorePin(CARD, PVV, PIN.length());
        assertThat(actualPin).isEqualTo(PIN);
    }
}