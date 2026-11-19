package com.skr1l.backend.warehouse.service;

import com.skr1l.backend.warehouse.dto.WarehouseRequestDTO;
import com.skr1l.backend.warehouse.dto.WarehouseResponseDTO;

import java.util.List;

public interface WarehouseService {

    Long createWarehouse(WarehouseRequestDTO dto);

    WarehouseResponseDTO getWarehouseById(Long id);

    List<WarehouseResponseDTO> getAllWarehouses();

    WarehouseResponseDTO updateWarehouse(Long id, WarehouseRequestDTO dto);

    void deleteWarehouse(Long id);
}
