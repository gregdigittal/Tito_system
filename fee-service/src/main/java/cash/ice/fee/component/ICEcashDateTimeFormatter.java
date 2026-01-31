package cash.ice.fee.component;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Component
public class ICEcashDateTimeFormatter {

    @Value("${ice.cash.group-api-date-format}")
    private String groupApiDateFormat;

    private SimpleDateFormat groupApiDateFormatter;

    @PostConstruct
    public void init() {
        groupApiDateFormatter = new SimpleDateFormat(groupApiDateFormat);
        groupApiDateFormatter.setTimeZone(TimeZone.getTimeZone("CAT"));
    }

    public String formatForGroupApi(Date date) {
        return groupApiDateFormatter.format(date);
    }
}
