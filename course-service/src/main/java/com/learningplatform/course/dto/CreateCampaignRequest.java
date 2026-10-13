package com.learningplatform.course.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateCampaignRequest {
    private String name;
    private BigDecimal discountPercentage;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private List<UUID> courseIds;
}
