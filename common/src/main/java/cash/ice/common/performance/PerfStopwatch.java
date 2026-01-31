package cash.ice.common.performance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
@JsonPropertyOrder({"min", "avg", "max", "total", "real-duration"})
public class PerfStopwatch {
    @JsonIgnore
    private Instant createInstant = Instant.now();
    @JsonIgnore
    private Instant start;
    @JsonIgnore
    private int count;
    @JsonIgnore
    private Duration min;
    @JsonIgnore
    private Duration max;
    @JsonIgnore
    private Duration avg;
    @JsonIgnore
    private Duration total = Duration.ZERO;
    @JsonIgnore
    private Duration realDuration;

    @JsonProperty("min")
    public String minJson() {
        return IceDuration.format(min);
    }

    @JsonProperty("max")
    public String maxJson() {
        return IceDuration.format(max);
    }

    @JsonProperty("avg")
    public String avgJson() {
        return IceDuration.format(avg);
    }

    @JsonProperty("total")
    public String totalJson() {
        return IceDuration.format(total);
    }

    @JsonProperty("real-duration")
    public String fullDurationJson() {
        return IceDuration.format(realDuration);
    }

    public void start() {
        start = Instant.now();
    }

    public void stop() {
        addStopwatch(Duration.between(start, Instant.now()));
    }

    public void addStopwatch(Duration duration) {
        if (min == null) {
            min = duration;
        } else {
            min = min.compareTo(duration) <= 0 ? min : duration;
        }
        if (max == null) {
            max = duration;
        } else {
            max = max.compareTo(duration) >= 0 ? max : duration;
        }
        total = total.plus(duration);
        count++;
        avg = count == 0 ? Duration.ZERO : total.dividedBy(count);
    }

    public synchronized void addStopwatch(Instant start, Instant end) {
        this.start = start;
        addStopwatch(Duration.between(start, end));
    }

    public PerfStopwatch finishStopwatch() {
        realDuration = Duration.between(createInstant, Instant.now());
        return this;
    }

    public void clear() {
        createInstant = Instant.now();
        start = null;
        count = 0;
        min = max = avg = realDuration = null;
        total = Duration.ZERO;
    }

    @Override
    public String toString() {
        return "PerfStopwatch {" +
                "AVG: " + (avg == null ? "0" : IceDuration.format(avg)) +
//                ", MIN: " + (min == null ? "0" : IceDuration.format(min)) +
//                ", MAX: " + (max == null ? "0" : IceDuration.format(max)) +
                ", TOTAL: " + (total == null ? "0" : IceDuration.format(total)) +
//                ", REAL: " + (realDuration == null ? "0" : IceDuration.format(realDuration)) +
//                ", count: " + count +
                '}';
    }
}
