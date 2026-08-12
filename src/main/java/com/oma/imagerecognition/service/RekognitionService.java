package com.oma.imagerecognition.service;

import com.oma.imagerecognition.config.AwsProperties;
import com.oma.imagerecognition.config.RekognitionImageProperties;
import com.oma.imagerecognition.dto.request.Base64CompareDTO;
import com.oma.imagerecognition.dto.request.Base64ImageDTO;
import com.oma.imagerecognition.dto.request.DownloadedImage;
import com.oma.imagerecognition.dto.request.ImageCompareDTO;
import com.oma.imagerecognition.dto.request.ImageCompareUrlDTO;
import com.oma.imagerecognition.dto.response.ImageResult;
import com.oma.imagerecognition.exception.ApiResponseException;
import com.oma.imagerecognition.model.RecognitionLabel;
import com.oma.imagerecognition.redis.model.RekognitionImageResult;
import com.oma.imagerecognition.repository.RedisRekognitionImageResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CompareFacesMatch;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RekognitionService {

    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;
    private final S3Service s3Service;
    private final DownloaderService downloaderService;
    private final RekognitionImageProperties rekognitionImageProperties;
    private final RedisRekognitionImageResultRepository imageResultRepository;

    @Value("${redis.image-result-expiration-hours}")
    private long imageResultExpirationHours;

    public List<RecognitionLabel> detectLabelsInS3Image(String objectKey) {
        S3Object s3Object = getS3Object(awsProperties.getS3BucketName(), objectKey);

        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(Image.builder().s3Object(s3Object).build())
                .maxLabels(10)
                .minConfidence(75F)
                .build();

        DetectLabelsResponse detectLabelsResponse = rekognitionClient.detectLabels(request);

        return detectLabelsResponse.labels()
                .stream()
                .map(label -> RecognitionLabel.builder()
                        .name(label.name())
                        .confidence(label.confidence())
                        .build()
                ).toList();
    }

    public ImageResult compareFaces(ImageCompareDTO imageCompareDTO, String requestId) {
        String bucket = awsProperties.getS3BucketName();

        S3Object srcImageS3Object = getS3Object(bucket, imageCompareDTO.getImageKey());
        S3Object tgtImgS3Object = getS3Object(bucket, imageCompareDTO.getTargetKey());

        Image source = getImage(srcImageS3Object);
        Image target = getImage(tgtImgS3Object);

        return imageResult(source, target, requestId);
    }

    public ImageResult compareFaces(MultipartFile sourceFile, MultipartFile targetFile, String requestId) {
        String sourceFileContentType = sourceFile.getContentType();
        boolean allowedSourceContentType = s3Service.isAllowedContentType(sourceFileContentType);
        String targetFileContentType = targetFile.getContentType();
        boolean allowedTargetContentType = s3Service.isAllowedContentType(targetFileContentType);

        if (!allowedSourceContentType) {
            throw new IllegalArgumentException("Unsupported Content-Type: " + sourceFileContentType);
        }
        if (!allowedTargetContentType) {
            throw new IllegalArgumentException("Unsupported Content-Type: " + targetFileContentType);
        }
        try {
            Image source = Image.builder()
                    .bytes(SdkBytes.fromByteArray(sourceFile.getBytes()))
                    .build();

            Image target = Image.builder()
                    .bytes(SdkBytes.fromByteArray(targetFile.getBytes()))
                    .build();

            return imageResult(source, target, requestId);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded images", e);
        }
    }

    public ImageResult compareFacesBase64(Base64CompareDTO dto, String requestId) {
        Base64ImageDTO src = dto.getSource();
        Base64ImageDTO tgt = dto.getTarget();

        String sourceContentType = src.getContentType();
        String targetContentType = tgt.getContentType();

        if (!s3Service.isAllowedContentType(sourceContentType)) {
            throw new IllegalArgumentException("Unsupported Content-Type: " + sourceContentType);
        }
        if (!s3Service.isAllowedContentType(targetContentType)) {
            throw new IllegalArgumentException("Unsupported Content-Type: " + targetContentType);
        }

        try {
            log.info("Decoding base64");

            byte[] sourceBytes = decodeBase64Strict(src.getBase64());
            byte[] targetBytes = decodeBase64Strict(tgt.getBase64());

            int size = rekognitionImageProperties.getMaxSize();

            // Optional but recommended: enforce Rekognition bytes limit (~5MB)
            enforceMaxBytes(sourceBytes, size * 1024 * 1024, "source");
            enforceMaxBytes(targetBytes, size * 1024 * 1024, "target");

            Image source = Image.builder()
                    .bytes(SdkBytes.fromByteArray(sourceBytes))
                    .build();

            Image target = Image.builder()
                    .bytes(SdkBytes.fromByteArray(targetBytes))
                    .build();


            return imageResult(source, target, requestId);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid base64 image payload", e);
        }
    }

    public ImageResult compareFacesImageUrl(ImageCompareUrlDTO dto, String requestId) {
        if (StringUtils.isBlank(dto.getSourceUrl()) || StringUtils.isBlank(dto.getTargetUrl())) {
            throw new IllegalArgumentException("URL is required");
        }

        try {
            URI srcUri = URI.create(dto.getSourceUrl());
            URI tgtUri = URI.create(dto.getTargetUrl());

            DownloadedImage src = downloaderService.fetch(srcUri);
            DownloadedImage tgt = downloaderService.fetch(tgtUri);

            byte[] srcBytes = src.getBytes();
            byte[] tgtBytes = tgt.getBytes();

            log.info("src.getBytes(): {}", srcBytes.length);

            Image source = Image.builder()
                    .bytes(SdkBytes.fromByteArray(srcBytes))
                    .build();

            Image target = Image.builder()
                    .bytes(SdkBytes.fromByteArray(tgtBytes))
                    .build();

            return imageResult(source, target, requestId);
        } catch (Exception e) {
            log.error("Error: ", e);
            throw new ApiResponseException("Failed to fetch images from URLs");
        }
    }

    private ImageResult imageResult(Image source, Image target, String requestId) {
        CompareFacesRequest request = CompareFacesRequest.builder()
                .sourceImage(source)
                .targetImage(target)
                .similarityThreshold(50F)
                .build();

        CompareFacesResponse response;
        try {
            response = rekognitionClient.compareFaces(request);
        } catch (RekognitionException e) {
            log.error("Rekognition failed: status={}, code={}, message={}, requestId={}",
                    e.statusCode(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "n/a",
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(),
                    e.requestId(),
                    e
            );
            throw e;
        }

        float best = 0f;
        for (CompareFacesMatch match : response.faceMatches()) {
            Float sim = match.similarity();
            if (sim != null && sim > best) best = sim;
        }

        ImageResult imageResult = ImageResult.builder()
                .similarity(best)
                .passed(best >= awsProperties.getThreshold())
                .build();
        if (StringUtils.isNotBlank(requestId)) {
            RekognitionImageResult rekognitionImageResult = RekognitionImageResult.builder()
                    .requestId(requestId)
                    .passed(imageResult.isPassed())
                    .similarity(imageResult.getSimilarity())
                    .ttl(Duration.ofHours(imageResultExpirationHours).getSeconds())
                    .build();
            imageResultRepository.save(rekognitionImageResult);
        }
        return imageResult;
    }

    private byte[] decodeBase64Strict(String rawBase64) {
        if (rawBase64 == null || rawBase64.isBlank()) {
            throw new IllegalArgumentException("base64 image is empty");
        }

        // If client mistakenly sends a data URL, reject clearly
        if (rawBase64.startsWith("data:")) {
            throw new IllegalArgumentException("Send raw base64 only (no data URL prefix).");
        }

        String cleaned = rawBase64.replaceAll("\\s+", "");

        return Base64.getDecoder().decode(cleaned);
    }

    private void enforceMaxBytes(byte[] bytes, int max, String label) {
        if (bytes.length > max) {
            throw new IllegalArgumentException(
                    label + " image is too large after decoding: " + bytes.length + " bytes (max " + max + ")"
            );
        }
    }

    private S3Object getS3Object(String bucket, String key) {
        return  S3Object.builder()
                .bucket(bucket)
                .name(key)
                .build();
    }

    private Image getImage(S3Object s3Object) {
        return Image.builder()
                .s3Object(s3Object)
                .build();
    }
}