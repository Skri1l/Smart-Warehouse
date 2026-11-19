package com.skr1l.backend.device.service;

import com.skr1l.backend.device.dto.DeviceRequestDto;
import com.skr1l.backend.device.dto.DeviceResponseDto;

import java.util.List;

public interface DeviceService {

    Long createDevice(DeviceRequestDto dto);

    DeviceResponseDto getDeviceById(Long id);

    List<DeviceResponseDto> getAllDevices();

    DeviceResponseDto updateDevice(Long id, DeviceRequestDto dto);

    void deleteDeviceById(Long id);

    void activateDevice(Long id);

    void deactivateDevice(Long id);

    void heartbeat(Long id);
}