package com.oma.imagerecognition.controller;

import com.oma.imagerecognition.dto.response.ImageUriResponse;
import com.oma.imagerecognition.dto.response.SessionResponse;
import com.oma.imagerecognition.dto.response.ErrorResponse;
import com.oma.imagerecognition.dto.response.SessionImageResult;
import com.oma.imagerecognition.response.CustomResponse;
import com.oma.imagerecognition.service.SessionRekognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/liveness/session")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SessionRekognitionController {

    private final SessionRekognitionService sessionRekognitionService;

    @Operation(summary = "Create Liveness Session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/create-session")
    public CustomResponse<SessionResponse> createSession(@RequestParam String requestId) {
        log.info("Create session");
        return new CustomResponse<>("Session Created successfully", "success", sessionRekognitionService.createSession(requestId));
    }

    @Operation(summary = "Fetch session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/get-session")
    public CustomResponse<SessionResponse> getSessionId(@RequestHeader(value = "session-token") String sessionToken) {
        log.info("Get session");
        return new CustomResponse<>("Session fetched successfully", "success", sessionRekognitionService.getAwsSession(sessionToken));
    }

    @Operation(summary = "Face session verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/verify-session")
    public CustomResponse<SessionImageResult> faceVerify(@RequestHeader(value = "session-token") String sessionToken) {
        log.info("Verifying session");
        return new CustomResponse<>("Face session verification completed successfully", "success", sessionRekognitionService.verifySession(sessionToken));
    }

    @Operation(summary = "Fetch Image Url")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/image-uri")
    public CustomResponse<ImageUriResponse> fetchImageUrl(@RequestParam String requestId) {
        log.info("Fetching image uri, request id: {}", requestId);
        return new CustomResponse<>("Image uri fetched successfully", "success", sessionRekognitionService.imageUriResponse(requestId));
    }
}
