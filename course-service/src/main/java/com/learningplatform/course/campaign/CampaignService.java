package com.learningplatform.course.campaign;

import com.learningplatform.course.dto.CreateCampaignRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Cacheable(value = "active_campaigns", key = "'active'")
    public List<Campaign> getActiveCampaigns() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        return campaignRepository.findByIsActiveTrue().stream()
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                .toList();
    }

    @CacheEvict(value = "active_campaigns", allEntries = true)
    public Campaign createCampaign(CreateCampaignRequest request) {
        Campaign campaign = Campaign.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .discountPercentage(request.getDiscountPercentage())
                .startDate(request.getStartDate() != null ? request.getStartDate() : java.time.ZonedDateTime.now())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();
        return campaignRepository.save(campaign);
    }

    @CacheEvict(value = "active_campaigns", allEntries = true)
    public void deleteCampaign(UUID id) {
        campaignRepository.deleteById(id);
    }

    @CacheEvict(value = "active_campaigns", allEntries = true)
    public Campaign toggleCampaign(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with ID: " + id));
        campaign.setIsActive(!Boolean.TRUE.equals(campaign.getIsActive()));
        return campaignRepository.save(campaign);
    }
}
