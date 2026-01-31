package cash.ice.api.service.impl;

import cash.ice.api.errors.SecurityPvvException;
import cash.ice.api.service.SecurityPvvService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static cash.ice.common.error.ErrorCodes.EC1006;
import static cash.ice.common.error.ErrorCodes.EC1007;

@Service
@Slf4j
public class SecurityPvvServiceImpl implements SecurityPvvService {
    private static final String ALGORITHM = "TripleDES";
    private static final String TRANSFORMATION_ALGORITHM = "TripleDES/ECB/NoPadding";

    @Value("${ice.cash.pvv.hex-cipher-key}")
    String hexCipherKey;

    private Cipher encryptCipher;
    private Cipher decryptCipher;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        String key = hexCipherKey;
        if (hexCipherKey.length() < 48) {
            key += hexCipherKey.substring(0, 48 - hexCipherKey.length());
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(HexFormat.of().parseHex(key), ALGORITHM);
        encryptCipher = Cipher.getInstance(TRANSFORMATION_ALGORITHM);
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        decryptCipher = Cipher.getInstance(TRANSFORMATION_ALGORITHM);
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
    }

    @Override
    public String acquirePvv(String number, String pin) {
        log.debug("  Acquiring PVV for number: {}", number);
        try {
            String pan = getPan(pin, number);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(HexFormat.of().parseHex(pan));
            return HexFormat.of().formatHex(encryptedMessageBytes).toUpperCase();
        } catch (Exception e) {
            throw new SecurityPvvException(EC1006, "Pvv getting error for: " + number, e);
        }
    }

    @Override
    public String restorePan(String pvv) {
        try {
            byte[] decryptedMessageBytes = decryptCipher.doFinal(HexFormat.of().parseHex(pvv));
            return HexFormat.of().formatHex(decryptedMessageBytes).toUpperCase();
        } catch (Exception e) {
            throw new SecurityPvvException(EC1007, "Pan getting error for: " + pvv, e);
        }
    }

    @Override
    public String restorePin(String number, String pvv, int digitsAmount) {
        try {
            return undoPin(number, digitsAmount, restorePan(pvv));
        } catch (Exception e) {
            throw new SecurityPvvException(EC1007, "PIN getting error for: " + number, e);
        }
    }

    private String getPan(String pin, String card) {
        String pinc = String.format("%02d", pin.length()) + pin;
        pinc += StringUtils.repeat('F', 16 - pinc.length());
        String pan = "0000" + card.substring(3, 15);
        return xorHex(pinc, pan);
    }

    private String undoPin(String card, int digitsAmount, String pan) {
        String ppan = "0000" + card.substring(3, 15);
        String xorPan = xorHex(ppan, pan);
        return xorPan.substring(2, 2 + digitsAmount);
    }

    private String xorHex(String hex1, String hex2) {
        if (Strings.isEmpty(hex1) || Strings.isEmpty(hex2) || hex1.length() != hex2.length()) {
            throw new IllegalArgumentException();
        }
        char[] chars = new char[hex1.length()];
        for (int i = 0; i < chars.length; i++) {
            int d = Character.digit(hex1.charAt(i), 16) ^ Character.digit(hex2.charAt(i), 16);
            chars[i] = Character.toUpperCase(Character.forDigit(d, 16));
        }
        return new String(chars);
    }
}
