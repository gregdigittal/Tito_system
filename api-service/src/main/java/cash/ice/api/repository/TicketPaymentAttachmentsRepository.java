package cash.ice.api.repository;

import cash.ice.api.dto.TicketPaymentAttachments;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TicketPaymentAttachmentsRepository extends MongoRepository<TicketPaymentAttachments, String> {

    List<TicketPaymentAttachments> findByVendorRefIn(List<String> vendorRefList);
}
