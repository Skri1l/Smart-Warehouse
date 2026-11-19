package com.skr1l.backend.imageEvent.service;

import com.skr1l.backend.imageEvent.dto.ImageEventRequestDto;
import com.skr1l.backend.imageEvent.dto.ImageEventResponseDto;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface ImageEventService {

    Long createImageEvent(ImageEventRequestDto dto);

    ImageEventResponseDto getImageEventById(Long id);

    List<ImageEventResponseDto> getAllImageEvents();

    List<ImageEventResponseDto> getImageEventsByDeviceId(Long deviceId);

    List<ImageEventResponseDto> getImageEventsByShelfId(Long shelfId);

    List<ImageEventResponseDto> getImageEventsByStatus(ImageProcessingStatus status);

    List<ImageEventResponseDto> getImageEventsByDateRange(LocalDateTime from, LocalDateTime to);

    void markAsProcessing(Long id);

    void markAsProcessed(Long id);

    void markAsFailed(Long id);

    void deleteImageEvent(Long id);
}