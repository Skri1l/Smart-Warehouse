package com.skr1l.backend.recognitionResult.service;

import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.recognitionResult.RecognitionResultIsNotFoundException;
import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.recognitionResult.RecognitionResult;
import com.skr1l.backend.recognitionResult.dto.RecognitionResultRequestDto;
import com.skr1l.backend.recognitionResult.dto.RecognitionResultResponseDto;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.repository.RecognitionResultRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecognitionResultServiceImpl implements RecognitionResultService {

    private static final AppLogger log = LoggerFactory.getLogger(RecognitionResultServiceImpl.class);

    private final RecognitionResultRepository recognitionResultRepository;

    private final ImageEventRepository imageEventRepository;

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long createRecognitionResult(RecognitionResultRequestDto dto) {
        log.info("createRecognitionResult");

        Objects.requireNonNull(dto, "dto is null");

        ImageEvent imageEvent = getImageEventEntityById(dto.imageEventId());
        Product product = getProductEntityById(dto.productId());

        RecognitionResult result = new RecognitionResult();
        result.setImageEvent(imageEvent);
        result.setProduct(product);
        result.setDetectedQuantity(dto.detectedQuantity());
        result.setQuantityDelta(dto.quantityDelta());
        result.setConfidence(dto.confidence());
        result.setStatus(RecognitionStatus.DETECTED);

        recognitionResultRepository.save(result);

        return result.getId();
    }

    @Override
    public RecognitionResultResponseDto getRecognitionResultById(Long id) {
        log.info("getRecognitionResultById");

        Objects.requireNonNull(id, "id is null");

        return toResponseDto(getRecognitionResultEntityById(id));
    }

    @Override
    public List<RecognitionResultResponseDto> getAllRecognitionResults() {
        log.info("getAllRecognitionResults");

        return recognitionResultRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<RecognitionResultResponseDto> getRecognitionResultsByImageEventId(Long imageEventId) {
        log.info("getRecognitionResultsByImageEventId");

        Objects.requireNonNull(imageEventId, "imageEventId is null");

        return recognitionResultRepository.findByImageEventId(imageEventId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<RecognitionResultResponseDto> getRecognitionResultsByProductId(Long productId) {
        log.info("getRecognitionResultsByProductId");

        Objects.requireNonNull(productId, "productId is null");

        return recognitionResultRepository.findByProductId(productId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<RecognitionResultResponseDto> getRecognitionResultsByStatus(RecognitionStatus status) {
        log.info("getRecognitionResultsByStatus");

        Objects.requireNonNull(status, "status is null");

        return recognitionResultRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<RecognitionResultResponseDto> getRecognitionResultsByMinConfidence(Double confidence) {
        log.info("getRecognitionResultsByMinConfidence");

        Objects.requireNonNull(confidence, "confidence is null");

        return recognitionResultRepository.findByConfidenceGreaterThanEqual(confidence)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void confirmRecognitionResult(Long id) {
        log.info("confirmRecognitionResult");

        RecognitionResult result = getRecognitionResultEntityById(id);
        result.setStatus(RecognitionStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void rejectRecognitionResult(Long id) {
        log.info("rejectRecognitionResult");

        RecognitionResult result = getRecognitionResultEntityById(id);
        result.setStatus(RecognitionStatus.REJECTED);
    }

    @Override
    @Transactional
    public void markRecognitionResultAsFailed(Long id) {
        log.info("markRecognitionResultAsFailed");

        RecognitionResult result = getRecognitionResultEntityById(id);
        result.setStatus(RecognitionStatus.FAILED);
    }

    @Override
    @Transactional
    public void deleteRecognitionResult(Long id) {
        log.info("deleteRecognitionResult");

        RecognitionResult result = getRecognitionResultEntityById(id);
        recognitionResultRepository.delete(result);
    }

    private RecognitionResult getRecognitionResultEntityById(Long id) {
        return recognitionResultRepository.findById(id)
                .orElseThrow(() -> new RecognitionResultIsNotFoundException(
                        "Recognition result with id " + id + " is not found"));
    }

    private ImageEvent getImageEventEntityById(Long id) {
        return imageEventRepository.findById(id)
                .orElseThrow(() -> new ImageEventIsNotFoundException(
                        "Image event with id " + id + " is not found"));
    }

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductIsNotFoundException(
                        "Product with id " + id + " is not found"));
    }

    private RecognitionResultResponseDto toResponseDto(RecognitionResult result) {
        return new RecognitionResultResponseDto(
                result.getId(),
                result.getImageEvent().getId(),
                result.getProduct().getId(),
                result.getDetectedQuantity(),
                result.getQuantityDelta(),
                result.getConfidence(),
                result.getStatus());
    }
}