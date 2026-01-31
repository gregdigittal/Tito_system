package cash.ice.common.performance;

import lombok.AllArgsConstructor;

import java.text.NumberFormat;
import java.time.Duration;

@AllArgsConstructor
public class IceDuration {
    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getNumberInstance();
    private static final NumberFormat FRACTION_FORMAT = NumberFormat.getNumberInstance();

    static {
        INTEGER_FORMAT.setGroupingUsed(true);
        FRACTION_FORMAT.setGroupingUsed(true);
        FRACTION_FORMAT.setMinimumIntegerDigits(9);
    }

    private Duration duration;

    public static String format(Duration duration) {
        return INTEGER_FORMAT.format(duration.getSeconds()) + "s, " + FRACTION_FORMAT.format(duration.getNano()) + "ns";
    }

    @Override
    public String toString() {
        return format(duration);
    }
}
