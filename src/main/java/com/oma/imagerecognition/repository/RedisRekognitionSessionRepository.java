package com.oma.imagerecognition.repository;

import com.oma.imagerecognition.redis.model.RekognitionSession;
import org.springframework.data.repository.CrudRepository;

public interface RedisRekognitionSessionRepository extends CrudRepository<RekognitionSession, String> {
}
