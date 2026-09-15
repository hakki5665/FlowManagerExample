package com.example.flowmanager.service.core;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface IStorageService {
    String upload(MultipartFile file, String objectName);

    InputStream download(String fullObjectPath);
}