package com.coworking.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private Double amount;
    private LocalDateTime issuedAt;
    private String paymentStatus;
    private Long bookingId;
}
