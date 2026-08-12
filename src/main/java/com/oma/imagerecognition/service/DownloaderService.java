package com.oma.imagerecognition.service;

import com.oma.imagerecognition.config.RekognitionImageProperties;
import com.oma.imagerecognition.dto.request.DownloadedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloaderService {
    private final S3Service s3Service;
    private final RestTemplate restTemplate;
    private final ValidatorService validatorService;
    private final RekognitionImageProperties rekognitionImageProperties;

    public DownloadedImage fetch(URI uri) {
        URI safeUri = validatorService.validatePublicUrl(uri);

        headCheck(safeUri);

        return getAndValidate(safeUri);
    }

    private DownloadedImage getAndValidate(URI uri) {
        long maxBytes = rekognitionImageProperties.getMaxSize() * 1024L * 1024L;
        RequestCallback requestCallback = request -> {
            HttpHeaders h = request.getHeaders();
            h.set(HttpHeaders.USER_AGENT, "face-compare-service/1.0");
            h.setAccept(List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG));
        };

        ResponseExtractor<DownloadedImage> extractor = response -> {
            if (response.getStatusCode().isError()) {
                throw new IllegalArgumentException("Failed to fetch image. HTTP " + response.getStatusCode());
            }

            HttpHeaders headers = response.getHeaders();

            MediaType mt = headers.getContentType();
            if (mt == null) throw new IllegalArgumentException("Missing Content-Type");
            String ct = mt.toString().toLowerCase();
            if (!s3Service.isAllowedContentType(ct)) throw new IllegalArgumentException("Unsupported Content-Type: " + ct);

            long contentLength = headers.getContentLength();
            if (contentLength > maxBytes) {
                throw new IllegalArgumentException("Image too large: " + contentLength);
            }

            try (InputStream is = response.getBody()) {

                // Read with a strict limit even if server lies about length
                byte[] bytes = readWithLimit(is, maxBytes);

                // Deep validation: ensure it is an actual decodable image (not HTML/zip/etc)
                // This parses the bytes and will fail for most non-image payloads.
                if (!isRealImage(bytes)) {
                    throw new IllegalArgumentException("Payload is not a valid image");
                }

                return DownloadedImage.builder()
                        .bytes(bytes)
                        .contentType(ct)
                        .build();
            }
        };

        return restTemplate.execute(uri, HttpMethod.GET, requestCallback, extractor);
    }

    private byte[] readWithLimit(InputStream is, long max) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;

        int read;
        while ((read = is.read(buf)) != -1) {
            total += read;
            if (total > max) throw new IllegalArgumentException("Image exceeded max size: " + max + " bytes");
            bos.write(buf, 0, read);
        }

        byte[] out = bos.toByteArray();
        if (out.length == 0) throw new IllegalArgumentException("Empty image");
        return out;
    }

    private boolean isRealImage(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bis);
            return img != null;
        } catch (IOException e) {
            return false;
        }
    }

    private void headCheck(URI uri) {
        long maxBytes = rekognitionImageProperties.getMaxSize() * 1024L * 1024L;
        try {
            HttpHeaders headers = restTemplate.headForHeaders(uri);
            log.info("Headers: {}", headers);

            MediaType ct = headers.getContentType();
            if (ct != null) {
                String cts = ct.toString().toLowerCase();
                if (!s3Service.isAllowedContentType(cts)) {
                    throw new IllegalArgumentException("URL content-type not allowed: " + cts);
                }
            }

            long len = headers.getContentLength();
            log.info("Content-Length: {}, maxBytes: {}", len, maxBytes);
            if (len > maxBytes) {
                throw new IllegalArgumentException("Image too large (Content-Length): " + len);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid header");
        }
    }

}
