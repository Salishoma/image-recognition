package com.oma.imagerecognition.service;

import com.oma.imagerecognition.config.AwsProperties;
import com.oma.imagerecognition.dto.response.ImageUriResponse;
import com.oma.imagerecognition.dto.response.SessionImageResult;
import com.oma.imagerecognition.dto.response.SessionResponse;
import com.oma.imagerecognition.exception.InvalidRequestException;
import com.oma.imagerecognition.model.BoundingBoxData;
import com.oma.imagerecognition.model.ProcessedAuditImage;
import com.oma.imagerecognition.redis.model.RekognitionImage;
import com.oma.imagerecognition.redis.model.RekognitionSession;
import com.oma.imagerecognition.repository.RedisRekognitionImageRepository;
import com.oma.imagerecognition.repository.RedisRekognitionSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.AuditImage;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.ChallengePreference;
import software.amazon.awssdk.services.rekognition.model.ChallengeType;
import software.amazon.awssdk.services.rekognition.model.CreateFaceLivenessSessionRequest;
import software.amazon.awssdk.services.rekognition.model.CreateFaceLivenessSessionRequestSettings;
import software.amazon.awssdk.services.rekognition.model.CreateFaceLivenessSessionResponse;
import software.amazon.awssdk.services.rekognition.model.GetFaceLivenessSessionResultsRequest;
import software.amazon.awssdk.services.rekognition.model.GetFaceLivenessSessionResultsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRekognitionService {

    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;
    private final RedisRekognitionSessionRepository rekognitionSessionRepository;
    private final RedisRekognitionImageRepository rekognitionImageRepository;

    public SessionResponse createSession(String requestId) {
        String sessionToken = UUID.randomUUID().toString();

        ChallengePreference challengePreference = ChallengePreference.builder()
                .type(ChallengeType.FACE_MOVEMENT_AND_LIGHT_CHALLENGE)
                .build();

        CreateFaceLivenessSessionRequestSettings settings = CreateFaceLivenessSessionRequestSettings.builder()
                .challengePreferences(Collections.singletonList(challengePreference))
                .build();

        CreateFaceLivenessSessionRequest request = CreateFaceLivenessSessionRequest.builder()
                .settings(settings)
                .build();

        CreateFaceLivenessSessionResponse response =
                rekognitionClient.createFaceLivenessSession(request);

        String awsSessionId = response.sessionId();
        RekognitionSession session = RekognitionSession.builder()
                .sessionToken(sessionToken)
                .awsSessionId(awsSessionId)
                .createdAt(Instant.now())
                .requestId(requestId)
                .ttl(awsProperties.getSessionExpirationTime() * 60)
                .used(false)
                .build();

        rekognitionSessionRepository.save(session);

        log.info("Created Rekognition session with token: {}", sessionToken);

        return SessionResponse.builder()
                .sessionToken(sessionToken)
                .build();
    }

    public SessionResponse getAwsSession(String sessionToken) {
        RekognitionSession session = validateSessionToken(sessionToken);

        return SessionResponse.builder()
                .awsSessionId(session.getAwsSessionId())
                .build();
    }

    public SessionImageResult verifySession(String sessionToken) {
        RekognitionSession session = validateSessionToken(sessionToken);

        GetFaceLivenessSessionResultsRequest request =
                GetFaceLivenessSessionResultsRequest.builder()
                        .sessionId(session.getAwsSessionId())
                        .build();

        GetFaceLivenessSessionResultsResponse result =
                rekognitionClient.getFaceLivenessSessionResults(request);

        SessionImageResult.SessionImageResultBuilder builder = SessionImageResult.builder();

        String imageDataUri = "";
        AuditImage image = result.referenceImage();
        log.info("Reference image: {}", image);
        if (image != null) {
            byte[] imageBytes = image.bytes().asByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            log.info("base64Image: {}", base64Image);

            imageDataUri = "data:image/jpeg;base64," + base64Image;
            builder.base64Image(imageDataUri);
        } else if (result.hasAuditImages()) {
            List<ProcessedAuditImage> processedAuditImages = new ArrayList<>();
            List<AuditImage> auditImages = result.auditImages();

            for (AuditImage auditImage : auditImages) {
                byte[] imageBytes = auditImage.bytes().asByteArray();

                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String dataUri = "data:image/jpeg;base64," + base64Image;

                BoundingBox boundingBox = auditImage.boundingBox();
                BoundingBoxData boundingBoxData = new BoundingBoxData();
                boundingBoxData.setWidth(boundingBox.width());
                boundingBoxData.setHeight(boundingBox.height());
                boundingBoxData.setLeft(boundingBox.left());
                boundingBoxData.setTop(boundingBox.top());

                ProcessedAuditImage processedImage = new ProcessedAuditImage();
                processedImage.setBase64Image(base64Image);
                processedImage.setDataUri(dataUri);
                processedImage.setBoundingBox(boundingBoxData);
                processedImage.setTimestamp(System.currentTimeMillis()); // Or use auditImage timestamp if available

                processedAuditImages.add(processedImage);
            }
            builder.auditImages(processedAuditImages);
        }

        Float confidence = result.confidence();
        String status = result.statusAsString();

        log.info("Confidence: {}", confidence);
        log.info("Status: {}", status);

        synchronized (sessionToken.intern()) {
            if (session.isUsed()) {
                throw new InvalidRequestException("Session token already verified");
            }
            session.setUsed(true);
            rekognitionSessionRepository.save(session);

            RekognitionImage rekognitionImage = RekognitionImage.builder()
                    .imageKey(session.getRequestId())
                    .imageUri(imageDataUri)
                    .ttl(awsProperties.getImageExpirationTime() * 60)
                    .build();

            rekognitionImageRepository.save(rekognitionImage);
        }

        boolean passed = confidence != null && confidence >= awsProperties.getThreshold();
        log.info("Session {} verified: status={}, confidence={}, passed={}", sessionToken, status, confidence, passed);

        return builder
                .confidence(confidence)
                .status(status)
                .sessionId(result.sessionId())
                .passed(passed)
                .build();
    }

    private RekognitionSession validateSessionToken(String sessionToken) {
        if (StringUtils.isBlank(sessionToken)) {
            throw new InvalidRequestException("sessionToken cannot be null or empty");
        }

        RekognitionSession session = rekognitionSessionRepository.findById(sessionToken)
                .orElseThrow(() ->  new InvalidRequestException("Invalid or expired session token"));

        if (session.isUsed()) {
            throw new InvalidRequestException("Session token already used");
        }
        return session;
    }

    public ImageUriResponse imageUriResponse(String requestId) {
        RekognitionImage rekognitionImage = rekognitionImageRepository.findById(requestId)
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired image key"));

        return ImageUriResponse.builder()
                .requestId(rekognitionImage.getImageKey())
                .imageUri(rekognitionImage.getImageUri())
                .build();
    }

}