package com.learningplatform.media.controller;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.media.Media;
import com.learningplatform.media.dto.*;
import com.learningplatform.media.service.VideoMultipartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/media/multipart", "/api/admin/videos/multipart"})
@RequiredArgsConstructor
public class VideoMultipartController {

    private final VideoMultipartService videoMultipartService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<InitiateMultipartResponse>> initiateMultipart(
            @RequestHeader(value = com.learningplatform.common.constants.SecurityHeaders.USER_EMAIL_HEADER, required = false) String userEmailHeader,
            @RequestBody InitiateMultipartRequest request
    ) {
        String userEmail = (userEmailHeader != null && !userEmailHeader.isBlank()) ? userEmailHeader : "admin";
        InitiateMultipartResponse response = videoMultipartService.initiateMultipart(request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Multipart video upload session initiated", response));
    }

    @PostMapping("/part-url")
    public ResponseEntity<ApiResponse<PartUrlResponse>> getPartUrl(
            @RequestBody PartUrlRequest request
    ) {
        PartUrlResponse response = videoMultipartService.getPartUrl(request);
        return ResponseEntity.ok(ApiResponse.success("Presigned part URL generated", response));
    }

    @PutMapping(value = "/part-chunk", consumes = {org.springframework.http.MediaType.ALL_VALUE})
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> uploadPartChunk(
            @RequestParam UUID uploadSessionId,
            @RequestParam Integer partNumber,
            @RequestBody byte[] chunkData,
            jakarta.servlet.http.HttpServletResponse httpServletResponse
    ) {
        String etag = videoMultipartService.uploadPartChunk(uploadSessionId, partNumber, chunkData);
        httpServletResponse.setHeader("ETag", "\"" + etag + "\"");
        httpServletResponse.setHeader("Access-Control-Expose-Headers", "ETag, etag, Authorization, Content-Type");
        return ResponseEntity.ok(ApiResponse.success("Chunk uploaded successfully", java.util.Map.of("partNumber", partNumber, "etag", etag)));
    }

    @GetMapping("/parts")
    public ResponseEntity<ApiResponse<UploadedPartsResponse>> listUploadedParts(
            @RequestParam UUID uploadSessionId
    ) {
        UploadedPartsResponse response = videoMultipartService.listUploadedParts(uploadSessionId);
        return ResponseEntity.ok(ApiResponse.success("Uploaded parts retrieved from R2", response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Media>> completeMultipart(
            @RequestBody CompleteMultipartRequest request
    ) {
        Media completedMedia = videoMultipartService.completeMultipartUpload(request);
        return ResponseEntity.ok(ApiResponse.success("Video multipart upload completed successfully", completedMedia));
    }

    @PostMapping("/abort")
    public ResponseEntity<ApiResponse<String>> abortMultipart(
            @RequestParam UUID uploadSessionId
    ) {
        videoMultipartService.abortMultipartUpload(uploadSessionId);
        return ResponseEntity.ok(ApiResponse.success("Multipart video upload aborted", "ABORTED"));
    }
}
