package com.skr1l.backend.warehouse.service;

import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.warehouse.Warehouse;
import com.skr1l.backend.warehouse.dto.WarehouseRequestDTO;
import com.skr1l.backend.warehouse.dto.WarehouseResponseDTO;
import com.skr1l.backend.warehouse.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final static AppLogger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public Long createWarehouse(WarehouseRequestDTO dto) {
        log.info("createWarehouse");
        Objects.requireNonNull(dto, "dto is null");

        Warehouse warehouse = new Warehouse();
        warehouse.setAddress(dto.address());
        warehouse.setName(dto.name());

        warehouseRepository.save(warehouse);

        return warehouse.getId();
    }

    @Override
    public WarehouseResponseDTO getWarehouseById(Long id) {
        log.info("getWarehouseById");
        Objects.requireNonNull(id, "id is null");

        Warehouse warehouse = getWarehouseEntityById(id);

        return toResponseDTO(warehouse);
    }

    @Override
    public List<WarehouseResponseDTO> getAllWarehouses() {
        log.info("getAllWarehouses");

        return warehouseRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public WarehouseResponseDTO updateWarehouse(Long id, WarehouseRequestDTO dto) {
        log.info("updateWarehouse");
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        Warehouse warehouse = getWarehouseEntityById(id);

        warehouse.setName(dto.name());
        warehouse.setAddress(dto.address());

        return toResponseDTO(warehouse);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long id) {
        log.info("deleteWarehouse");
        Objects.requireNonNull(id, "id is null");

        Warehouse warehouse = getWarehouseEntityById(id);

        warehouseRepository.delete(warehouse);
    }

    private Warehouse getWarehouseEntityById(Long id) {
        log.info("getWarehouseEntityById");
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseIsNotFoundException("Warehouse is not found"));
    }

    private WarehouseResponseDTO toResponseDTO(Warehouse warehouse) {
        log.info("toResponseDTO");
        return new WarehouseResponseDTO(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getAddress()
        );
    }
}
