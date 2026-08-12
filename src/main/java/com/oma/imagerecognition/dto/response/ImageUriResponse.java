package com.oma.imagerecognition.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ImageUriResponse {
    private String requestId;
    private String imageUri;
}
