package com.oma.imagerecognition.repository;

import com.oma.imagerecognition.redis.model.RekognitionImage;
import org.springframework.data.repository.CrudRepository;

public interface RedisRekognitionImageRepository extends CrudRepository<RekognitionImage, String> {
}
