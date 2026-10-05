package com.skr1l.backend.rack.service;

import com.skr1l.backend.rack.dto.RackRequestDto;
import com.skr1l.backend.rack.dto.RackResponseDto;
import java.util.List;

public interface RackService {

    Long createRack(RackRequestDto dto, Long warehouseId);

    RackResponseDto getRackById(Long id);

    List<RackResponseDto> getAllRacks();

    RackResponseDto updateRack(RackRequestDto dto, Long rackId, Long warehouseId);

    void deleteRackById(Long id);
}
