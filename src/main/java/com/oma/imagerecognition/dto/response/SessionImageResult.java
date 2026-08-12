package com.oma.imagerecognition.dto.response;

import com.oma.imagerecognition.model.ProcessedAuditImage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SessionImageResult {
    private Float confidence;
    private String status;
    private String sessionId;
    private boolean passed;
    private String base64Image;
    private List<ProcessedAuditImage> auditImages;
}
