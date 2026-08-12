package com.oma.imagerecognition.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Base64ImageDTO {
    private String contentType;
    private String base64;
}
