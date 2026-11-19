package com.skr1l.backend.device.service;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.dto.DeviceRequestDto;
import com.skr1l.backend.device.dto.DeviceResponseDto;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceAlreadyExistsException;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.deviceExceptions.ShelfAlreadyHasDeviceException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final AppLogger log = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private final DeviceRepository deviceRepository;

    private final ShelfRepository shelfRepository;

    @Override
    @Transactional
    public Long createDevice(DeviceRequestDto dto) {
        log.info("createDevice");

        Objects.requireNonNull(dto, "dto is null");

        if (deviceRepository.existsByDeviceKey(dto.deviceKey())) {
            throw new DeviceAlreadyExistsException(
                    "Device with key " + dto.deviceKey() + " already exists");
        }

        if (deviceRepository.existsByShelfId(dto.shelfId())) {
            throw new ShelfAlreadyHasDeviceException(
                    "Shelf with id " + dto.shelfId() + " already has a device");
        }

        Shelf shelf = getShelfEntityById(dto.shelfId());

        Device device = new Device();
        device.setDeviceKey(dto.deviceKey());
        device.setShelf(shelf);
        device.setStatus(DeviceStatus.INACTIVE);
        device.setLastSeenAt(null);

        deviceRepository.save(device);

        return device.getId();
    }

    @Override
    public DeviceResponseDto getDeviceById(Long id) {
        log.info("getDeviceById");

        Objects.requireNonNull(id, "id is null");

        Device device = getDeviceEntityById(id);

        return toResponseDto(device);
    }

    @Override
    public List<DeviceResponseDto> getAllDevices() {
        log.info("getAllDevices");

        return deviceRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public DeviceResponseDto updateDevice(Long id, DeviceRequestDto dto) {
        log.info("updateDevice");

        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        Device device = getDeviceEntityById(id);

        if (deviceRepository.existsByDeviceKeyAndIdNot(dto.deviceKey(), id)) {
            throw new DeviceAlreadyExistsException(
                    "Device with key " + dto.deviceKey() + " already exists");
        }

        if (deviceRepository.existsByShelfIdAndIdNot(dto.shelfId(), id)) {
            throw new ShelfAlreadyHasDeviceException(
                    "Shelf with id " + dto.shelfId() + " already has a device");
        }

        Shelf shelf = getShelfEntityById(dto.shelfId());

        device.setDeviceKey(dto.deviceKey());
        device.setShelf(shelf);

        return toResponseDto(device);
    }

    @Override
    @Transactional
    public void deleteDeviceById(Long id) {
        log.info("deleteDeviceById");

        Objects.requireNonNull(id, "id is null");

        Device device = getDeviceEntityById(id);

        deviceRepository.delete(device);
    }

    @Override
    @Transactional
    public void activateDevice(Long id) {
        log.info("activateDevice");

        Objects.requireNonNull(id, "id is null");

        Device device = getDeviceEntityById(id);

        device.setStatus(DeviceStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void deactivateDevice(Long id) {
        log.info("deactivateDevice");

        Objects.requireNonNull(id, "id is null");

        Device device = getDeviceEntityById(id);

        device.setStatus(DeviceStatus.INACTIVE);
    }

    @Override
    @Transactional
    public void heartbeat(Long id) {
        log.info("heartbeat");

        Objects.requireNonNull(id, "id is null");

        Device device = getDeviceEntityById(id);

        device.setLastSeenAt(LocalDateTime.now());
        device.setStatus(DeviceStatus.ACTIVE);
    }

    private Device getDeviceEntityById(Long id) {
        log.info("getDeviceEntityById");

        return deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceIsNotFoundException(
                                "Device with id " + id + " is not found"));
    }

    private Shelf getShelfEntityById(Long id) {
        log.info("getShelfEntityById");

        return shelfRepository.findById(id)
                .orElseThrow(() -> new ShelfIsNotFoundException(
                                "Shelf with id " + id + " is not found"));
    }

    private DeviceResponseDto toResponseDto(Device device) {
        log.info("deviceToResponseDto");

        return new DeviceResponseDto(
                device.getId(),
                device.getDeviceKey(),
                device.getStatus(),
                device.getLastSeenAt(),
                device.getShelf().getId());
    }
}