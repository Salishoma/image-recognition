package com.oma.imagerecognition.exception;

import com.oma.imagerecognition.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import software.amazon.awssdk.services.rekognition.model.InvalidImageFormatException;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.InvalidS3ObjectException;
import software.amazon.awssdk.services.rekognition.model.AccessDeniedException;
import software.amazon.awssdk.services.rekognition.model.ThrottlingException;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.InternalServerErrorException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handleNotFound(NoHandlerFoundException ex) {
        log.error("Exception in handleNotFound: {}", ex.getMessage());
        String requestURL = ex.getRequestURL();
        return new ResponseEntity<>(new ErrorResponse("Resource not found in " + requestURL + ". Check your url and try again",
                HttpStatus.NOT_FOUND.name()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = InvalidParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameterException(InvalidParameterException ex) {
        log.error("Exception in handleInvalidParameterException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = InvalidS3ObjectException.class)
    public ResponseEntity<ErrorResponse> handleInvalidS3ObjectException(InvalidS3ObjectException ex) {
        log.error("Exception in handleInvalidS3ObjectException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Exception in handleIllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Bad Request", HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = InvalidImageFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImageFormatException(InvalidImageFormatException ex) {
        log.error("Exception in handleInvalidImageFormatException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.error("Exception in handleMethodArgumentTypeMismatchException: {}", ex.getMessage());
        String name = ex.getName();
        return new ResponseEntity<>(new ErrorResponse("Invalid value for parameter: " + name, HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        log.error("Exception in handleBindException: {}", ex.getMessage());
        String name = ex.getFieldError() != null ? ex.getFieldError().getField(): "parameter";
        return new ResponseEntity<>(new ErrorResponse("Invalid value for parameter: " + name, HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        log.error("Exception in handleHttpClientError: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Unauthorized Access", "unauthorized"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = InvalidCredentialException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialException(InvalidCredentialException ex) {
        log.error("Exception in InvalidCredentialException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), "credential_error"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccess(ResourceAccessException ex) {
        log.error("Exception in handleResourceAccess: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Service Unavailable: ", HttpStatus.SERVICE_UNAVAILABLE.name()), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<?> handleRestClientException(RestClientException ex) {
        log.error("Exception in handleRestClientException: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse("Error communicating with downstream service: ", HttpStatus.BAD_GATEWAY.name()), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<?> handleConversionException(HttpMessageConversionException ex) {
        log.error("Exception in handleConversionException: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse("Failed to process response from external service: ", HttpStatus.BAD_GATEWAY.name()), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("Exception in handleMissingServletRequestParameterException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(ex.getParameterName() +" required", HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptableException(Exception ex) {
        log.error("Exception in handleNotAcceptableException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Unsupported media type requested. " +
                "Try Accept: text/csv.", HttpStatus.NOT_ACCEPTABLE.name()), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(value = ApiResponseException.class)
    public ResponseEntity<ErrorResponse> handleApiResponseException(ApiResponseException ex) {
        log.error("Exception in handleApiResponseException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(InvalidRequestException ex) {
        log.error("Exception in handleInvalidRequestException≈: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.name()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.error("Exception in handleMissingRequestHeaderException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN.name()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Exception in handleAccessDeniedException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Liveness feature not enabled for your account", HttpStatus.FORBIDDEN.name()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Exception in handleResourceNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("The sessionId does NOT exist", HttpStatus.NOT_FOUND.name()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = ThrottlingException.class)
    public ResponseEntity<ErrorResponse> handleThrottlingException(ThrottlingException ex) {
        log.error("Exception in handleThrottlingException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Too many requests per second", HttpStatus.TOO_MANY_REQUESTS.name()), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(value = RekognitionException.class)
    public ResponseEntity<ErrorResponse> handleRekognitionException(RekognitionException ex) {
        log.error("Exception in handleApiRekognitionException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Exception occurred during facial recognition", HttpStatus.INTERNAL_SERVER_ERROR.name()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = InternalServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleInternalServerErrorException(InternalServerErrorException ex) {
        log.error("Exception in handleInternalServerErrorException: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("AWS internal failure: ", HttpStatus.INTERNAL_SERVER_ERROR.name()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Exception in GlobalExceptionHandler: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse("Something went wrong: ", HttpStatus.INTERNAL_SERVER_ERROR.name()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

