package com.coworking.booking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "معرف المستخدم مطلوب")
    @Column(nullable = false)
    private Long userId;

    @NotNull(message = "التقييم مطلوب")
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer rating;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;
}