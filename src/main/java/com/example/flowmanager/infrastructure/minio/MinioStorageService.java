package com.example.flowmanager.infrastructure.minio;

import com.example.flowmanager.service.core.IStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements IStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    @SneakyThrows
    public String upload(MultipartFile file, String objectName) {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String fullObjectName = objectName + extension;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullObjectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return bucketName + "/" + fullObjectName;
    }

    @Override
    @SneakyThrows
    public InputStream download(String fullObjectPath) {
        if (fullObjectPath == null || fullObjectPath.isBlank()) {
            throw new IllegalArgumentException("Путь к файлу в хранилище не может быть пустым");
        }

        String objectName = fullObjectPath;
        if (fullObjectPath.startsWith(bucketName + "/")) {
            objectName = fullObjectPath.substring(bucketName.length() + 1);
        }

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}