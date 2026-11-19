package com.skr1l.backend.recognitionResult.service;

import com.skr1l.backend.recognitionResult.dto.RecognitionResultRequestDto;
import com.skr1l.backend.recognitionResult.dto.RecognitionResultResponseDto;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import java.util.List;

public interface RecognitionResultService {

    Long createRecognitionResult(RecognitionResultRequestDto dto);

    RecognitionResultResponseDto getRecognitionResultById(Long id);

    List<RecognitionResultResponseDto> getAllRecognitionResults();

    List<RecognitionResultResponseDto> getRecognitionResultsByImageEventId(Long imageEventId);

    List<RecognitionResultResponseDto> getRecognitionResultsByProductId(Long productId);

    List<RecognitionResultResponseDto> getRecognitionResultsByStatus(RecognitionStatus status);

    List<RecognitionResultResponseDto> getRecognitionResultsByMinConfidence(Double confidence);

    void confirmRecognitionResult(Long id);

    void rejectRecognitionResult(Long id);

    void markRecognitionResultAsFailed(Long id);

    void deleteRecognitionResult(Long id);
}