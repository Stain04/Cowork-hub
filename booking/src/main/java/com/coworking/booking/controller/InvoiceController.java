package com.coworking.booking.controller;

import com.coworking.booking.dto.InvoiceDTO;
import com.coworking.booking.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<InvoiceDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(invoiceService.updatePaymentStatus(id, status));
    }
}
