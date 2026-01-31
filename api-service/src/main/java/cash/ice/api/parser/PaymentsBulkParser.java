package cash.ice.api.parser;

import cash.ice.api.entity.zim.Payment;
import cash.ice.sqldb.entity.PaymentLine;

import java.io.InputStream;
import java.util.List;

public interface PaymentsBulkParser {

    List<PaymentLine> parseExcelStream(InputStream inputStream, Payment payment);
}
