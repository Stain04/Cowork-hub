package com.coworking.booking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "workspaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "اسم الغرفة مطلوب")
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "نوع الغرفة مطلوب")
    @Column(nullable = false)
    private WorkspaceType type; 

    private String description;

    @NotNull(message = "السعر بالساعة مطلوب")
    @Min(value = 0, message = "السعر لا يمكن أن يكون بالسالب")
    @Column(nullable = false)
    private Double pricePerHour;

    @NotNull(message = "السعة المطلوبة")
    @Min(value = 1, message = "يجب أن تستوعب الغرفة شخصاً واحداً على الأقل")
    @Column(nullable = false)
    private Integer capacity;

    @Builder.Default
    private boolean available = true;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}
