package com.oma.imagerecognition.service;

import com.oma.imagerecognition.config.AwsProperties;
import com.oma.imagerecognition.dto.response.SessionImageResult;
import com.oma.imagerecognition.dto.response.SessionResponse;
import com.oma.imagerecognition.redis.model.RekognitionSession;
import com.oma.imagerecognition.repository.RedisRekognitionImageRepository;
import com.oma.imagerecognition.repository.RedisRekognitionSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateFaceLivenessSessionRequest;
import software.amazon.awssdk.services.rekognition.model.CreateFaceLivenessSessionResponse;
import software.amazon.awssdk.services.rekognition.model.GetFaceLivenessSessionResultsRequest;
import software.amazon.awssdk.services.rekognition.model.GetFaceLivenessSessionResultsResponse;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(SessionRekognitionService.class)
class SessionRekognitionServiceTest {
    @MockBean
    private RekognitionClient rekognitionClient;

    @MockBean
    private AwsProperties awsProperties;

    @MockBean
    private RedisRekognitionSessionRepository sessionRepository;

    @MockBean
    private RedisRekognitionImageRepository imageRepository;

    @Autowired
    private SessionRekognitionService service;

    @Test
    void testCreateSession() {
        String fakeAwsSessionId = "aws-session-123";
        CreateFaceLivenessSessionResponse awsResponse =
                CreateFaceLivenessSessionResponse.builder().sessionId(fakeAwsSessionId).build();

        when(rekognitionClient.createFaceLivenessSession(any(CreateFaceLivenessSessionRequest.class)))
                .thenReturn(awsResponse);

        SessionResponse backendToken = service.createSession("requestId");

        verify(sessionRepository).save(any());
        assertNotNull(backendToken);
    }

    @Test
    void testVerifySession() {
        String token = "backend-token";
        String awsSessionId = "aws-session-123";

        RekognitionSession session = RekognitionSession.builder()
                .sessionToken(token)
                .awsSessionId(awsSessionId)
                .used(false)
                .createdAt(Instant.now())
                .ttl(300)
                .build();

        when(sessionRepository.findById(token)).thenReturn(Optional.of(session));

        GetFaceLivenessSessionResultsResponse awsResult =
                GetFaceLivenessSessionResultsResponse.builder()
                        .confidence(0.95f)
                        .status("PASSED")
                        .sessionId(awsSessionId)
                        .build();

        when(rekognitionClient.getFaceLivenessSessionResults(any(GetFaceLivenessSessionResultsRequest.class)))
                .thenReturn(awsResult);

        SessionImageResult result = service.verifySession(token);

        assertEquals("PASSED", result.getStatus());
        assertTrue(result.isPassed());
        assertEquals(awsSessionId, result.getSessionId());
        verify(sessionRepository).save(any());
        verify(imageRepository).save(any());
    }


}