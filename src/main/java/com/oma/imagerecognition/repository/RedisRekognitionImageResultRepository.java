package com.oma.imagerecognition.repository;

import com.oma.imagerecognition.redis.model.RekognitionImageResult;
import org.springframework.data.repository.CrudRepository;

public interface RedisRekognitionImageResultRepository extends CrudRepository<RekognitionImageResult, String> {
}
