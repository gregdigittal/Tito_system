package cash.ice.api.service;

import cash.ice.api.dto.moz.LinkNfcTagRequest;
import cash.ice.api.dto.moz.TagInfoMoz;
import cash.ice.sqldb.entity.moz.Device;

public interface DeviceLinkMozService {

    Device linkPosDevice(String posDeviceSerial, Integer entityId, String otp);

    Device linkPosDeviceToVehicle(String posDeviceSerial, int authEntityId, Integer vehicleId);

    TagInfoMoz linkNfcTag(LinkNfcTagRequest nfcTag, String otp);

    /** Delink NFC tag from account; tag returns to Unassigned. Caller must own the account. */
    TagInfoMoz delinkNfcTag(String identifier, Integer authEntityId);
}
