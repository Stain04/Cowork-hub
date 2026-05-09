package com.coworking.booking.service;

import com.coworking.booking.dto.BookingRequest;
import com.coworking.booking.dto.BookingResponse;
import com.coworking.booking.model.Booking;
import com.coworking.booking.model.Invoice;
import com.coworking.booking.model.Workspace;
import com.coworking.booking.repository.BookingRepository;
import com.coworking.booking.repository.InvoiceRepository;
import com.coworking.booking.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new RuntimeException("Start time and end time are required.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime().isBefore(now)) {
            throw new RuntimeException("Start time must be in the future.");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time.");
        }

        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new RuntimeException("Selected workspace was not found."));

        boolean isOverlapping = bookingRepository.existsOverlappingBooking(
                request.getWorkspaceId(), request.getStartTime(), request.getEndTime());

        if (isOverlapping) {
            throw new RuntimeException("This workspace is already booked for the selected time range.");
        }

        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        double hours = minutes / 60.0;
        double totalAmount = hours * workspace.getPricePerHour();

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .workspace(workspace)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalAmount(totalAmount)
                .status("PENDING")
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(totalAmount)
                .issuedAt(LocalDateTime.now())
                .paymentStatus("UNPAID")
                .booking(savedBooking)
                .build();

        invoiceRepository.save(invoice);

        return BookingResponse.builder()
                .bookingId(savedBooking.getId())
                .workspaceName(workspace.getName())
                .startTime(savedBooking.getStartTime())
                .endTime(savedBooking.getEndTime())
                .totalAmount(savedBooking.getTotalAmount())
                .status(savedBooking.getStatus())
                .invoiceNumber(invoice.getInvoiceNumber())
                .build();
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(booking -> BookingResponse.builder()
                        .bookingId(booking.getId())
                        .workspaceName(booking.getWorkspace().getName())
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .totalAmount(booking.getTotalAmount())
                        .status(booking.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking was not found."));

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking is already cancelled.");
        }

        Invoice invoice = invoiceRepository.findByBookingId(bookingId).orElse(null);
        if (invoice != null) {
            invoiceRepository.delete(invoice);
            booking.setInvoice(null);
        }

        booking.setStatus("CANCELLED");
        Booking savedBooking = bookingRepository.save(booking);

        return BookingResponse.builder()
                .bookingId(savedBooking.getId())
                .workspaceName(savedBooking.getWorkspace().getName())
                .startTime(savedBooking.getStartTime())
                .endTime(savedBooking.getEndTime())
                .totalAmount(savedBooking.getTotalAmount())
                .status(savedBooking.getStatus())
                .invoiceNumber(null)
                .build();
    }
}
