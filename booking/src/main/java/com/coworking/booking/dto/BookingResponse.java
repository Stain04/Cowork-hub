package com.coworking.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long bookingId;
    private String workspaceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalAmount;
    private String status;
    private String invoiceNumber;
}