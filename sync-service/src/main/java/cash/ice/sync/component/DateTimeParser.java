package cash.ice.sync.component;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

@Component
public class DateTimeParser {
    private static final String BIRTH_DATE_PATTERN = "[yyyyMMdd][yyyy-MM-dd][yyyy/MM/dd][dd/M/yyyy][dd MMM yyyy][MMM dd yyyy][MMM  d yyyy]";
    private static final DateTimeFormatter BIRTH_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern(BIRTH_DATE_PATTERN)
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter BIRTH_DATE_ALT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[ddMMyyyy][dd-MMM-yy][dd/M/yy]")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter BIRTH_DATE_ALT2_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyyddMM]")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
            .toFormatter();

    public LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }

    public LocalDate parseBirthDate(String birthDateStr) {
        if (ObjectUtils.isEmpty(birthDateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(birthDateStr, BIRTH_DATE_FORMATTER);
        } catch (DateTimeException e) {
            try {
                LocalDate localDate = LocalDate.parse(birthDateStr, BIRTH_DATE_ALT_FORMATTER);
                return localDate.isAfter(LocalDate.now()) ? localDate.minusYears(100) : localDate;
            } catch (DateTimeException e2) {
                return LocalDate.parse(birthDateStr, BIRTH_DATE_ALT2_FORMATTER);
            }
        }
    }
}
