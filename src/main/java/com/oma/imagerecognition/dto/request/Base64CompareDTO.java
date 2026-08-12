package com.oma.imagerecognition.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Base64CompareDTO {
    private Base64ImageDTO source;
    private Base64ImageDTO target;
}
