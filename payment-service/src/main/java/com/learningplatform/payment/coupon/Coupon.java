package com.learningplatform.payment.coupon;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "expiration_date")
    private ZonedDateTime expirationDate;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (maxUses == null) maxUses = 100;
        if (usedCount == null) usedCount = 0;
    }
}
