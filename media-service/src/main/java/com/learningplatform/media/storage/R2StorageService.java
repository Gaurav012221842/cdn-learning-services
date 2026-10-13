package com.learningplatform.media.storage;

import com.learningplatform.media.config.R2StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final R2StorageConfig r2StorageConfig;

    public String getBucketName() {
        return r2StorageConfig.getBucketName();
    }
}
