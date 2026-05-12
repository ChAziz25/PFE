package com.PFE.backend.services;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.BucketExistsArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:sandbox-outputs}")
    private String bucket;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                System.out.println("✓ MinIO bucket '" + bucket + "' created successfully.");
            } else {
                System.out.println("✓ MinIO bucket '" + bucket + "' already exists.");
            }
        } catch (Exception e) {
            System.err.println("✗ Error ensuring MinIO bucket exists: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void uploadFile(String filename, InputStream inputStream, long size) {
        try {
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .stream(inputStream, size, -1)
                            .build()
            );
            System.out.println("✓ File '" + filename + "' uploaded to MinIO successfully.");
        } catch (Exception e) {
            System.err.println("✗ Error uploading file to MinIO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error uploading file to MinIO: " + e.getMessage());
        }
    }

    public InputStream downloadFile(String filename) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("✗ Error downloading file from MinIO: " + e.getMessage());
            throw new RuntimeException("Error downloading file from MinIO: " + e.getMessage());
        }
    }

    public void deleteFile(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .build()
            );
            System.out.println("✓ File '" + filename + "' deleted from MinIO successfully.");
        } catch (Exception e) {
            System.err.println("✗ Error deleting file from MinIO: " + e.getMessage());
            throw new RuntimeException("Error deleting file from MinIO: " + e.getMessage());
        }
    }
}