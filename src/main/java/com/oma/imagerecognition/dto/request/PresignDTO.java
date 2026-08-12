package com.oma.imagerecognition.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PresignDTO {
    private String presignKey;
    private String contentType;
}
