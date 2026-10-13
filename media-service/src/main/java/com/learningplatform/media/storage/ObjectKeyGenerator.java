package com.learningplatform.media.storage;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ObjectKeyGenerator {

    public String generateKey(String fileName, String prefix) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i);
        }
        return prefix + "/" + UUID.randomUUID() + extension;
    }
}
