package com.coworking.booking.service;

import com.coworking.booking.dto.InvoiceDTO;
import com.coworking.booking.model.Invoice;
import com.coworking.booking.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
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

    private InvoiceDTO mapToDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount())
                .issuedAt(invoice.getIssuedAt())
                .paymentStatus(invoice.getPaymentStatus())
                .bookingId(invoice.getBooking().getId())
                .build();
    }
}
