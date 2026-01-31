package cash.ice.api.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.BiFunction;

public final class CsvUtil {
    private CsvUtil() {
    }

    public static <T> String listToCsv(Iterable<T> list, CSVFormat format, BiFunction<T, Integer, List<String>> dataConverter) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format);
        int i = 0;
        for (T obj : list) {
            csvPrinter.printRecord(dataConverter.apply(obj, ++i));
        }
        csvPrinter.flush();
        return out.toString();
    }

    public static CSVFormat createCsvFormat(boolean header, String[] csvHeader, Character delimiter, String rowDelimiter) {
        CSVFormat format = CSVFormat.DEFAULT;
        if (header) {
            format = format.withHeader(csvHeader);
        }
        if (delimiter != null) {
            format = format.withDelimiter(delimiter);
        }
        if (rowDelimiter != null) {
            format = format.withRecordSeparator(rowDelimiter);
        }
        return format;
    }

}
