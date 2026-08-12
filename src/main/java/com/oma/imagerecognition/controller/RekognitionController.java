package com.oma.imagerecognition.controller;

import com.oma.imagerecognition.dto.request.Base64CompareDTO;
import com.oma.imagerecognition.dto.request.ImageCompareDTO;
import com.oma.imagerecognition.dto.request.ImageCompareUrlDTO;
import com.oma.imagerecognition.dto.request.PresignDTO;
import com.oma.imagerecognition.dto.response.ErrorResponse;
import com.oma.imagerecognition.dto.response.ImageResult;
import com.oma.imagerecognition.dto.response.PresignResponse;
import com.oma.imagerecognition.model.RecognitionLabel;
import com.oma.imagerecognition.response.CustomResponse;
import com.oma.imagerecognition.service.RekognitionService;
import com.oma.imagerecognition.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/v1/verify")
public class RekognitionController {

    private final RekognitionService rekognitionService;
    private final S3Service s3Service;

    public RekognitionController(RekognitionService rekognitionService, S3Service s3Service) {
        this.rekognitionService = rekognitionService;
        this.s3Service = s3Service;
    }

    @Operation(summary = "Detect Labels")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
    })
    @GetMapping("/detect-labels")
    public CustomResponse<List<RecognitionLabel>> detectLabels(@RequestParam String objectKey) {
        return new CustomResponse<>("Labels detected successfully", "success", rekognitionService.detectLabelsInS3Image(objectKey));
    }

    @Operation(summary = "Presign Url")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
    })
    @PostMapping("/presign")
    public CustomResponse<PresignResponse> presign(@RequestBody PresignDTO presignDTO) {
        return new CustomResponse<>("Presigned Url generated successfully", "success",
                s3Service.generatePresignedUploadUrl(presignDTO, Duration.ofMinutes(5)));
    }

    @Operation(summary = "Compare Facial Images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping("/compare-faces")
    public CustomResponse<ImageResult> compare(
            @RequestBody ImageCompareDTO imageCompareDTO,
            @RequestParam(required = false) String requestId
    ) {
        return new CustomResponse<>("Image compared successfully", "success", rekognitionService.compareFaces(imageCompareDTO, requestId));
    }

    @Operation(summary = "Compare Facial Images via photo upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping(value = "/compare-faces/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomResponse<ImageResult> compare(
            @RequestPart("source") MultipartFile source,
            @RequestPart("target") MultipartFile target,
            @RequestParam(required = false) String requestId
    ) {
        return new CustomResponse<>("Image compared successfully", "success", rekognitionService.compareFaces(source, target, requestId));
    }

    @Operation(summary = "Compare Facial Images via Base64 bytes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping(value = "/compare-faces/base64")
    public CustomResponse<ImageResult> compare2(
            @RequestBody Base64CompareDTO dto,
            @RequestParam(required = false) String requestId
    ) {
        return new CustomResponse<>("Base64 Images compared successfully", "success", rekognitionService.compareFacesBase64(dto, requestId));
    }

    @Operation(summary = "Compare Facial Images via Image Url")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CustomResponse.class))}),
            @ApiResponse(responseCode = "400", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "401", content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PostMapping(value = "/compare-faces/image-url")
    public CustomResponse<ImageResult> compareFacesImageUrl(
            @RequestBody ImageCompareUrlDTO dto,
            @RequestParam(required = false) String requestId
    ) {
        return new CustomResponse<>("Images from urls compared successfully", "success", rekognitionService.compareFacesImageUrl(dto, requestId));
    }
}