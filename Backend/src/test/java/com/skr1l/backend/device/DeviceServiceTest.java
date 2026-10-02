package com.skr1l.backend.device;

import com.skr1l.backend.device.dto.DeviceRequestDto;
import com.skr1l.backend.device.dto.DeviceResponseDto;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.device.service.DeviceServiceImpl;
import com.skr1l.backend.exception.deviceExceptions.DeviceAlreadyExistsException;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.deviceExceptions.ShelfAlreadyHasDeviceException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private ShelfRepository shelfRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    private Shelf shelf;

    private Device device;

    private DeviceRequestDto dto;

    @BeforeEach
    void setUp() {
        shelf = new Shelf();
        shelf.setId(20L);

        device = new Device();
        device.setId(30L);
        device.setDeviceKey("CAM-001");
        device.setStatus(DeviceStatus.INACTIVE);
        device.setShelf(shelf);

        dto = new DeviceRequestDto("CAM-001", 20L);
    }

    @Test
    void createDeviceShouldReturnIdWhenDataIsValid() {
        // Arrange
        when(deviceRepository.existsByDeviceKey("CAM-001"))
                .thenReturn(false);

        when(deviceRepository.existsByShelfId(20L))
                .thenReturn(false);

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> {
                    Device savedDevice = invocation.getArgument(0);
                    savedDevice.setId(30L);
                    return savedDevice;
                });

        // Act
        Long result = deviceService.createDevice(dto);

        // Assert
        assertEquals(30L, result);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void createDeviceShouldThrowExceptionWhenDeviceKeyAlreadyExists() {
        // Arrange
        when(deviceRepository.existsByDeviceKey("CAM-001"))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(DeviceAlreadyExistsException.class,
                () -> deviceService.createDevice(dto));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void createDeviceShouldThrowExceptionWhenShelfAlreadyHasDevice() {
        // Arrange
        when(deviceRepository.existsByDeviceKey("CAM-001"))
                .thenReturn(false);

        when(deviceRepository.existsByShelfId(20L))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(ShelfAlreadyHasDeviceException.class,
                () -> deviceService.createDevice(dto));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void createDeviceShouldThrowExceptionWhenShelfDoesNotExist() {
        // Arrange
        when(deviceRepository.existsByDeviceKey("CAM-001"))
                .thenReturn(false);

        when(deviceRepository.existsByShelfId(20L))
                .thenReturn(false);

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(ShelfIsNotFoundException.class,
                () -> deviceService.createDevice(dto));
    }

    @Test
    void getDeviceByIdShouldReturnDeviceWhenDeviceExists() {
        // Arrange
        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        // Act
        DeviceResponseDto result = deviceService.getDeviceById(30L);

        // Assert
        assertEquals(30L, result.id());
        assertEquals("CAM-001", result.deviceKey());
        assertEquals(DeviceStatus.INACTIVE, result.status());
        assertEquals(20L, result.shelfId());
    }

    @Test
    void getDeviceByIdShouldThrowExceptionWhenDeviceDoesNotExist() {
        // Arrange
        when(deviceRepository.findById(30L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(DeviceIsNotFoundException.class,
                () -> deviceService.getDeviceById(30L));
    }

    @Test
    void getAllDevicesShouldReturnDevices() {
        // Arrange
        when(deviceRepository.findAll())
                .thenReturn(List.of(device));

        // Act
        List<DeviceResponseDto> result = deviceService.getAllDevices();

        // Assert
        assertEquals(1, result.size());
        assertEquals("CAM-001", result.getFirst().deviceKey());
    }

    @Test
    void updateDeviceShouldUpdateDevice() {
        // Arrange
        DeviceRequestDto updateDto = new DeviceRequestDto("CAM-002", 20L);

        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.existsByDeviceKeyAndIdNot("CAM-002", 30L))
                .thenReturn(false);

        when(deviceRepository.existsByShelfIdAndIdNot(20L, 30L))
                .thenReturn(false);

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        // Act
        DeviceResponseDto result = deviceService.updateDevice(30L, updateDto);

        // Assert
        assertEquals("CAM-002", result.deviceKey());
        assertEquals("CAM-002", device.getDeviceKey());
    }

    @Test
    void updateDeviceShouldThrowExceptionWhenDeviceKeyExists() {
        // Arrange
        DeviceRequestDto updateDto = new DeviceRequestDto("CAM-002", 20L);

        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.existsByDeviceKeyAndIdNot("CAM-002", 30L))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(DeviceAlreadyExistsException.class,
                () -> deviceService.updateDevice(30L, updateDto));
    }

    @Test
    void updateDeviceShouldThrowExceptionWhenShelfAlreadyHasDevice() {
        // Arrange
        DeviceRequestDto updateDto = new DeviceRequestDto("CAM-002", 20L);

        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.existsByDeviceKeyAndIdNot("CAM-002", 30L))
                .thenReturn(false);

        when(deviceRepository.existsByShelfIdAndIdNot(20L, 30L))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(ShelfAlreadyHasDeviceException.class,
                () -> deviceService.updateDevice(30L, updateDto));
    }

    @Test
    void activateDeviceShouldSetStatusActive() {
        // Arrange
        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        // Act
        deviceService.activateDevice(30L);

        // Assert
        assertEquals(DeviceStatus.ACTIVE, device.getStatus());
    }

    @Test
    void deactivateDeviceShouldSetStatusInactive() {
        // Arrange
        device.setStatus(DeviceStatus.ACTIVE);

        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        // Act
        deviceService.deactivateDevice(30L);

        // Assert
        assertEquals(DeviceStatus.INACTIVE, device.getStatus());
    }

    @Test
    void heartbeatShouldUpdateLastSeenAtAndActivateDevice() {
        // Arrange
        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        // Act
        deviceService.heartbeat(30L);

        // Assert
        assertNotNull(device.getLastSeenAt());
        assertEquals(DeviceStatus.ACTIVE, device.getStatus());
    }

    @Test
    void deleteDeviceShouldDeleteDevice() {
        // Arrange
        when(deviceRepository.findById(30L))
                .thenReturn(Optional.of(device));

        // Act
        deviceService.deleteDeviceById(30L);

        // Assert
        verify(deviceRepository).delete(device);
    }
}