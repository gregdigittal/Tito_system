package cash.ice.api.service;

public interface SecurityPvvService {

    String acquirePvv(String number, String pin);

    String restorePan(String pvv);

    String restorePin(String number, String pvv, int digitsAmount);
}
