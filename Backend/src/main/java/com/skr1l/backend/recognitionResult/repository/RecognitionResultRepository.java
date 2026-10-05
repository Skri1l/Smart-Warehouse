package com.skr1l.backend.recognitionResult.repository;

import com.skr1l.backend.recognitionResult.RecognitionResult;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecognitionResultRepository extends JpaRepository<RecognitionResult, Long> {

    List<RecognitionResult> findByImageEventId(Long imageEventId);

    List<RecognitionResult> findByProductId(Long productId);

    List<RecognitionResult> findByStatus(RecognitionStatus status);

    List<RecognitionResult> findByConfidenceGreaterThanEqual(Double confidence);
}