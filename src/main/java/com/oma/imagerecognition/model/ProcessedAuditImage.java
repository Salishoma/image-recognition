package com.oma.imagerecognition.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessedAuditImage {
    private String base64Image;
    private String dataUri;
    private BoundingBoxData boundingBox;
    private Long timestamp;
}
