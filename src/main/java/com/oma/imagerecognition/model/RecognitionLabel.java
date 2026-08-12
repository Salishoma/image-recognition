package com.oma.imagerecognition.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RecognitionLabel {
    public String name;
    public Float confidence;

}
