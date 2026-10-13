package com.learningplatform.media;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.media.dto.AvatarUploadRequest;
import com.learningplatform.media.dto.AvatarUploadResponse;
import com.learningplatform.media.dto.SignedUrlResponse;
import com.learningplatform.media.dto.UploadRequest;
import com.learningplatform.media.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<Media>>> getAllMedia() {
        return ResponseEntity.ok(ApiResponse.success("Media list retrieved successfully", mediaService.getAllMedia()));
    }

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadResponse>> requestUpload(@RequestBody UploadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Presigned upload URL generated", mediaService.requestUpload(request)));
    }

    @PostMapping("/confirm/{mediaId}")
    public ResponseEntity<ApiResponse<Media>> confirmUpload(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(ApiResponse.success("Media upload confirmed", mediaService.confirmUpload(mediaId)));
    }

    @GetMapping("/{id}/signed-url")
    public ResponseEntity<ApiResponse<SignedUrlResponse>> getSignedStreamingUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Signed streaming URL generated", mediaService.getSignedStreamingUrl(id)));
    }

    @GetMapping("/r2-status")
    public ResponseEntity<ApiResponse<com.learningplatform.media.dto.CloudflareR2StatusResponse>> getCloudflareStatus() {
        return ResponseEntity.ok(ApiResponse.success("Cloudflare connection status retrieved", mediaService.getCloudflareStatus()));
    }

    @PostMapping("/avatar-upload-url")
    public ResponseEntity<ApiResponse<AvatarUploadResponse>> generateAvatarUploadUrl(
            @RequestHeader(value = com.learningplatform.common.constants.SecurityHeaders.USER_EMAIL_HEADER, required = false) String userEmailHeader,
            @RequestBody AvatarUploadRequest request) {
        String userEmail = (userEmailHeader != null && !userEmailHeader.isBlank()) ? userEmailHeader : "user@gauravlearn.com";
        AvatarUploadResponse response = mediaService.generateAvatarUploadUrl(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Avatar upload URL generated", response));
    }

    @PostMapping("/upload-avatar-file")
    public ResponseEntity<ApiResponse<String>> uploadAvatarFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader(value = com.learningplatform.common.constants.SecurityHeaders.USER_EMAIL_HEADER, required = false) String userEmailHeader) {
        String userEmail = (userEmailHeader != null && !userEmailHeader.isBlank()) ? userEmailHeader : "user@gauravlearn.com";
        String cdnUrl = mediaService.uploadAvatarToCloudflareR2(userEmail, file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded to Cloudflare R2 successfully", cdnUrl));
    }

    @PostMapping(value = "/upload-file", consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "mediaType", required = false) String mediaTypeStr) {
        com.learningplatform.media.MediaType mediaType = null;
        if (mediaTypeStr != null && !mediaTypeStr.isBlank()) {
            try {
                mediaType = com.learningplatform.media.MediaType.valueOf(mediaTypeStr.toUpperCase());
            } catch (Exception ignored) {}
        }
        UploadResponse response = mediaService.uploadFileToCloudflareR2(file, mediaType);
        return ResponseEntity.ok(ApiResponse.success("Media asset uploaded to Cloudflare R2 successfully", response));
    }

    @PutMapping(value = "/upload-direct", consumes = {org.springframework.http.MediaType.ALL_VALUE})
    public ResponseEntity<ApiResponse<String>> uploadDirect(
            @RequestParam("objectKey") String objectKey,
            @RequestParam(value = "part", required = false) Integer part,
            @RequestParam(value = "uploadId", required = false) String uploadId,
            @RequestBody(required = false) byte[] fileData,
            jakarta.servlet.http.HttpServletResponse httpServletResponse) {
        String cdnUrl = mediaService.uploadDirectFile(objectKey, fileData);
        String etag = "\"etag-part-" + (part != null ? part : "1") + "-" + System.currentTimeMillis() + "\"";
        httpServletResponse.setHeader("ETag", etag);
        httpServletResponse.setHeader("Access-Control-Expose-Headers", "ETag, etag, Authorization, Content-Type");
        return ResponseEntity.ok(ApiResponse.success("Direct media upload successful", cdnUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(@PathVariable UUID id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok(ApiResponse.success("Media asset deleted successfully", id.toString()));
    }
}
