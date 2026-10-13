package com.learningplatform.course.campaign;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage;

    @Column(name = "start_date")
    private ZonedDateTime startDate;

    @Column(name = "end_date")
    private ZonedDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (isActive == null) isActive = true;
    }
}
