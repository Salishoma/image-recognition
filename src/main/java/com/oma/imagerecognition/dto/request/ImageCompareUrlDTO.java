package com.oma.imagerecognition.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageCompareUrlDTO {
    private String sourceUrl;
    private String targetUrl;
}
