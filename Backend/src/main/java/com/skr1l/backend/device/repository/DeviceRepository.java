package com.skr1l.backend.device.repository;

import com.skr1l.backend.device.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    boolean existsByDeviceKey(String deviceKey);

    boolean existsByShelfId(Long shelfId);

    boolean existsByDeviceKeyAndIdNot(String deviceKey, Long id);

    boolean existsByShelfIdAndIdNot(Long shelfId, Long id);
}