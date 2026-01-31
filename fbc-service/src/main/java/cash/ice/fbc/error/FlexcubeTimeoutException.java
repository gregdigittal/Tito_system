package cash.ice.fbc.error;

import lombok.Getter;

@Getter
public class FlexcubeTimeoutException extends RuntimeException {
    private final Integer referenceId;

    public FlexcubeTimeoutException(Integer referenceId) {
        this.referenceId = referenceId;
    }
}
