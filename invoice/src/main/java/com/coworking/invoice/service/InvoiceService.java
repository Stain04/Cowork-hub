package com.coworking.invoice.service;

import com.coworking.invoice.dto.InvoiceDTO;
import com.coworking.invoice.kafka.BookingCancelledEvent;
import com.coworking.invoice.kafka.BookingCreatedEvent;
import com.coworking.invoice.model.Invoice;
import com.coworking.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public InvoiceDTO getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice was not found."));
        return mapToDTO(invoice);
    }

    public InvoiceDTO getInvoiceByBookingId(Long bookingId) {
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Invoice was not found."));
        return mapToDTO(invoice);
    }

    public InvoiceDTO updatePaymentStatus(Long invoiceId, String newStatus) {
        if (!"PAID".equalsIgnoreCase(newStatus) && !"UNPAID".equalsIgnoreCase(newStatus)) {
            throw new RuntimeException("Payment status must be PAID or UNPAID.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice was not found."));

        invoice.setPaymentStatus(newStatus.toUpperCase());
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return mapToDTO(updatedInvoice);
    }

    public void createInvoiceForBooking(BookingCreatedEvent event) {
        if (event == null || event.bookingId() == null) {
            return;
        }

        boolean alreadyExists = invoiceRepository.findByBookingId(event.bookingId()).isPresent();
        if (alreadyExists) {
            return;
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(event.totalAmount())
                .issuedAt(event.createdAt() != null ? event.createdAt() : LocalDateTime.now())
                .paymentStatus("UNPAID")
                .bookingId(event.bookingId())
                .build();

        invoiceRepository.save(invoice);
    }

    public void cancelInvoiceForBooking(BookingCancelledEvent event) {
        if (event == null || event.bookingId() == null) {
            return;
        }
        invoiceRepository.deleteByBookingId(event.bookingId());
    }

    private InvoiceDTO mapToDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount())
                .issuedAt(invoice.getIssuedAt())
                .paymentStatus(invoice.getPaymentStatus())
                .bookingId(invoice.getBookingId())
                .build();
    }
}
