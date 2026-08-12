package com.oma.imagerecognition.service;

import com.oma.imagerecognition.config.AwsProperties;
import com.oma.imagerecognition.dto.request.PresignDTO;
import com.oma.imagerecognition.dto.response.PresignResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Set;

@Service
public class S3Service {

    private final S3Presigner presigner;
    private final AwsProperties awsProperties;

    public S3Service(AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
        this.presigner = S3Presigner.builder()
                .region(awsProperties.getRegion())
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    public PresignResponse generatePresignedUploadUrl(PresignDTO presignDTO, Duration validFor) {
        String contentType = presignDTO.getContentType();
        if (!isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported Content-Type: " + contentType);
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(awsProperties.getS3BucketName())
                .key(presignDTO.getPresignKey())
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(p -> p.signatureDuration(validFor)
                .putObjectRequest(request));

        return PresignResponse.builder()
                .presignedUrl(presigned.url().toExternalForm())
                .build();
    }

    public boolean isAllowedContentType(String contentType) {
        return Set.of(
                "image/jpeg",
                "image/png",
                "image/webp",
                "image/jpg"
        ).contains(contentType);
    }
}
