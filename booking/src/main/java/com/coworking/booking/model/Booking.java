package com.coworking.booking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "يجب تحديد المستخدم")
    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workspace_id", nullable = false)
    @NotNull(message = "يجب اختيار غرفة")
    private Workspace workspace;

    @NotNull(message = "تاريخ البداية مطلوب")
    @FutureOrPresent(message = "تاريخ البداية لا يمكن أن يكون في الماضي")
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "تاريخ النهاية مطلوب")
    @Future(message = "تاريخ النهاية يجب أن يكون في المستقبل")
    @Column(nullable = false)
    private LocalDateTime endTime;

    private Double totalAmount;

    @Column(nullable = false)
    private String status;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Invoice invoice;
}