package com.skr1l.backend.imageEvent.service;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.dto.ImageEventRequestDto;
import com.skr1l.backend.imageEvent.dto.ImageEventResponseDto;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelf.Shelf;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImageEventServiceImpl implements ImageEventService {

    private static final AppLogger log = LoggerFactory.getLogger(ImageEventServiceImpl.class);

    private final ImageEventRepository imageEventRepository;

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional
    public Long createImageEvent(ImageEventRequestDto dto) {
        log.info("createImageEvent");

        Objects.requireNonNull(dto, "dto is null");

        Device device = getDeviceEntityById(dto.deviceId());

        Shelf shelf = device.getShelf();

        ImageEvent imageEvent = new ImageEvent();

        imageEvent.setDevice(device);
        imageEvent.setShelf(shelf);
        imageEvent.setImagePath(dto.imagePath());
        imageEvent.setCapturedAt(dto.capturedAt());
        imageEvent.setReceivedAt(LocalDateTime.now());
        imageEvent.setImageProcessingStatus(ImageProcessingStatus.RECEIVED);

        imageEventRepository.save(imageEvent);

        return imageEvent.getId();
    }

    @Override
    public ImageEventResponseDto getImageEventById(Long id) {
        log.info("getImageEventById");

        Objects.requireNonNull(id, "id is null");

        return toResponseDto(getImageEventEntityById(id));
    }

    @Override
    public List<ImageEventResponseDto> getAllImageEvents() {
        log.info("getAllImageEvents");

        return imageEventRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ImageEventResponseDto>
    getImageEventsByDeviceId(Long deviceId) {
        log.info("getImageEventsByDeviceId");

        Objects.requireNonNull(deviceId, "deviceId is null");

        return imageEventRepository
                .findByDeviceId(deviceId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ImageEventResponseDto>
    getImageEventsByShelfId(Long shelfId) {
        log.info("getImageEventsByShelfId");

        Objects.requireNonNull(shelfId, "shelfId is null");

        return imageEventRepository
                .findByShelfId(shelfId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ImageEventResponseDto>
    getImageEventsByStatus(ImageProcessingStatus status) {
        log.info("getImageEventsByStatus");

        Objects.requireNonNull(status, "status is null");

        return imageEventRepository
                .findByImageProcessingStatus(status)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ImageEventResponseDto>
    getImageEventsByDateRange(LocalDateTime from, LocalDateTime to) {
        log.info("getImageEventsByDateRange");

        Objects.requireNonNull(from, "from is null");

        Objects.requireNonNull(to, "to is null");

        return imageEventRepository
                .findByCapturedAtBetween(from, to)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsProcessing(Long id) {
        log.info("markAsProcessing");

        ImageEvent imageEvent = getImageEventEntityById(id);

        imageEvent.setImageProcessingStatus(ImageProcessingStatus.PROCESSING);
    }

    @Override
    @Transactional
    public void markAsProcessed(Long id) {
        log.info("markAsProcessed");

        ImageEvent imageEvent = getImageEventEntityById(id);

        imageEvent.setImageProcessingStatus(ImageProcessingStatus.PROCESSED);
    }

    @Override
    @Transactional
    public void markAsFailed(Long id) {
        log.info("markAsFailed");

        ImageEvent imageEvent = getImageEventEntityById(id);

        imageEvent.setImageProcessingStatus(ImageProcessingStatus.FAILED);
    }

    @Override
    @Transactional
    public void deleteImageEvent(Long id) {
        log.info("deleteImageEvent");

        ImageEvent imageEvent = getImageEventEntityById(id);

        imageEventRepository.delete(imageEvent);
    }

    private ImageEvent getImageEventEntityById(Long id) {
        Objects.requireNonNull(id, "id is null");

        return imageEventRepository.findById(id).orElseThrow(() ->
                new ImageEventIsNotFoundException("Image event with id " + id + " is not found"));
    }

    private Device getDeviceEntityById(Long id) {
        return deviceRepository.findById(id).orElseThrow(() ->
                new DeviceIsNotFoundException("Device with id " + id + " is not found"));
    }

    private ImageEventResponseDto toResponseDto(ImageEvent imageEvent) {

        return new ImageEventResponseDto(
                imageEvent.getId(),
                imageEvent.getDevice().getId(),
                imageEvent.getShelf().getId(),
                imageEvent.getImagePath(),
                imageEvent.getCapturedAt(),
                imageEvent.getReceivedAt(),
                imageEvent.getImageProcessingStatus());
    }
}
