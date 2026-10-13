package com.learningplatform.course.pricing;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "course_pricing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePricing {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "discount_price")
    private BigDecimal discountPrice;

    @Column(length = 10)
    private String currency;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (currency == null) currency = "USD";
    }
}
