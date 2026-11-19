package com.skr1l.backend.rack.service;

import com.skr1l.backend.exception.rackExceptions.RackIsNotFoundException;
import com.skr1l.backend.exception.warehouseExceptions.WarehouseIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.rack.dto.RackRequestDto;
import com.skr1l.backend.rack.dto.RackResponseDto;
import com.skr1l.backend.rack.repository.RackRepository;
import com.skr1l.backend.warehouse.Warehouse;
import com.skr1l.backend.warehouse.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RackServiceImpl implements RackService {

    private final static AppLogger log = LoggerFactory.getLogger(RackServiceImpl.class);

    private final RackRepository rackRepository;

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public Long createRack(RackRequestDto dto, Long warehouseId) {
        log.info("createRack");
        Objects.requireNonNull(dto, "dto is null");
        Objects.requireNonNull(warehouseId, "warehouseId is null");

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseIsNotFoundException(
                        "Warehouse with id " + warehouseId + " is not found"));

        Rack rack = new Rack();
        rack.setLabel(dto.label());
        rack.setWarehouse(warehouse);

        rackRepository.save(rack);
        return rack.getId();
    }

    @Override
    public RackResponseDto getRackById(Long id) {
        log.info("getRackById");
        Objects.requireNonNull(id, "id is null");

        Rack rack = getRackEntityById(id);

        return toResponseDTO(rack);
    }

    @Override
    public List<RackResponseDto> getAllRacks() {
        log.info("getAllRacks");

        return rackRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public RackResponseDto updateRack(RackRequestDto dto, Long rackId, Long warehouseId) {
        log.info("updateRack");
        Objects.requireNonNull(dto, "dto is null");
        Objects.requireNonNull(rackId, "rackId is null");
        Objects.requireNonNull(warehouseId, "warehouseId is null");

        Rack rack = getRackEntityById(rackId);

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseIsNotFoundException(
                        "Warehouse with id " + warehouseId + " is not found"));

        rack.setLabel(dto.label());
        rack.setWarehouse(warehouse);

        return toResponseDTO(rack);
    }

    @Override
    @Transactional
    public void deleteRackById(Long id) {
        log.info("deleteRackById");
        Objects.requireNonNull(id, "id is null");

        Rack rack = getRackEntityById(id);

        rackRepository.delete(rack);

    }

    private RackResponseDto toResponseDTO(Rack rack) {
        log.info("rackToResponseDTO");
        return new RackResponseDto(
                rack.getId(),
                rack.getLabel(),
                rack.getWarehouse().getId());
    }

    private Rack getRackEntityById(Long id) {
        log.info("getRackEntityById");
        return rackRepository.findById(id)
                .orElseThrow(() -> new RackIsNotFoundException("Rack is not found"));
    }
}
