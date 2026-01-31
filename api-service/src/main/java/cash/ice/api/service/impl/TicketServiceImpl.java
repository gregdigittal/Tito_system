package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.TicketPaymentAttachments;
import cash.ice.api.repository.TicketPaymentAttachmentsRepository;
import cash.ice.api.service.TicketService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Ticket;
import cash.ice.sqldb.entity.TicketStatus;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.repository.TicketRepository;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {
    private final DeviceRepository deviceRepository;
    private final TicketRepository ticketRepository;
    private final TicketPaymentAttachmentsRepository ticketPaymentAttachmentsRepository;
    private final MozProperties mozProperties;

    @Override
    public void createTicketFor(List<PaymentResponse> failedPayments, List<PaymentRequest> paymentRequestList) {
        if (!failedPayments.isEmpty()) {
            List<TicketPaymentAttachments> existingAttachments = ticketPaymentAttachmentsRepository.findByVendorRefIn(failedPayments.stream().map(PaymentResponse::getVendorRef).toList());
            List<Integer> ticketIds = existingAttachments.stream().map(TicketPaymentAttachments::getTicketId).filter(Objects::nonNull).distinct().toList();
            List<String> existingVendorRefs = existingAttachments.stream().map(TicketPaymentAttachments::getVendorRef).toList();
            List<PaymentResponse> newPayments = failedPayments.stream().filter(paymentResponse -> !existingVendorRefs.contains(paymentResponse.getVendorRef())).toList();
            String deviceCode = (String) paymentRequestList.get(0).getMeta().get(PaymentMetaKey.DeviceCode);
            String deviceSerial = deviceCode != null ? deviceRepository.findByCode(deviceCode).map(Device::getSerial).orElse(null) : null;
            log.warn("  Failed {} offload transactions, deviceCode: {}, deviceSerial: {}, filling a ticket. {} new payments, {} already posted in {} tickets, ticket IDs: {}",
                    failedPayments.size(), deviceCode, deviceSerial, newPayments.size(), existingAttachments.size(), ticketIds.size(), ticketIds);
            Ticket ticket = ticketRepository.findAllById(ticketIds).stream().filter(ticket1 -> ticket1.getStatus() == TicketStatus.UNRESOLVED).findAny()
                    .orElse(new Ticket().setStatus(TicketStatus.UNRESOLVED));
            boolean ticketExists = ticket.getId() != null;

            Ticket savedTicket = ticketRepository.save(ticket
                    .setSubject(mozProperties.getOffloadFailTicketSubject().replace("<DEVICE_CODE>", Objects.toString(deviceCode)))
                    .setBody(mozProperties.getOffloadFailTicketBody().replace("<DEVICE_SERIAL>", Objects.toString(deviceSerial)))
                    .setPriority(mozProperties.getOffloadFailTicketPriority())
                    .setCreatedDate(Tool.currentDateTime()));
            log.debug((ticketExists ? "  using existing ticket with ID: " : "  creating new ticket: ") + savedTicket);
            newPayments.forEach(paymentResponse -> ticketPaymentAttachmentsRepository.save(new TicketPaymentAttachments()
                    .setVendorRef(paymentResponse.getVendorRef())
                    .setTicketId(savedTicket.getId())
                    .setStatus(TicketStatus.UNRESOLVED)
                    .setDescription("Failed offload transaction")
                    .setErrorCode(paymentResponse.getErrorCode())
                    .setErrorMessage(paymentResponse.getMessage())
                    .setPaymentRequest(paymentRequestList.stream().filter(request -> Objects.equals(request.getVendorRef(), paymentResponse.getVendorRef())).findAny().orElse(null))
                    .setCreatedDate(Tool.currentDateTime())));
        }
    }
}
