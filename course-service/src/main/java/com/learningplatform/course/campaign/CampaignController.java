package com.learningplatform.course.campaign;

import com.learningplatform.course.dto.CreateCampaignRequest;
import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Campaign>>> getActiveCampaigns() {
        return ResponseEntity.ok(ApiResponse.success("Campaigns retrieved", campaignService.getActiveCampaigns()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Campaign>>> getAllCampaigns() {
        return ResponseEntity.ok(ApiResponse.success("All campaigns retrieved", campaignService.getAllCampaigns()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Campaign>> createCampaign(@RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Campaign created", campaignService.createCampaign(request)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Campaign>> toggleCampaign(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Campaign status updated", campaignService.toggleCampaign(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCampaign(@PathVariable java.util.UUID id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.ok(ApiResponse.success("Campaign deleted successfully", "SUCCESS"));
    }
}
