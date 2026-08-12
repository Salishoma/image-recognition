package com.oma.imagerecognition.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoundingBoxData {
    private Float width;
    private Float height;
    private Float left;
    private Float top;

}