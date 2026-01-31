package cash.ice.api.dto.moz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MoneyProviderMoz {
    MPESA("mpesa", "MPI"),
    EMOLA("emola", "EMI");

    private final String initiatorType;
    private final String inboundTx;
}
