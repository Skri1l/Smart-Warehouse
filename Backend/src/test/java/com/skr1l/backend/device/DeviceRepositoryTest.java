package com.skr1l.backend.device;

import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Shelf shelf;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Test warehouse");
        warehouse.setAddress("Test address");

        entityManager.persist(warehouse);

        Rack rack = new Rack();
        rack.setLabel("RACK-TEST");
        rack.setWarehouse(warehouse);

        entityManager.persist(rack);

        shelf = new Shelf();
        shelf.setLabel("SHELF-TEST");
        shelf.setCapacity(100);
        shelf.setStatus(ShelfStatus.ACTIVE);
        shelf.setRack(rack);

        entityManager.persist(shelf);

        entityManager.flush();
    }

    @Test
    void existsByDeviceKeyShouldReturnTrueWhenDeviceExists() {
        // Arrange
        Device device = createDevice("DEVICE-TEST-001", shelf);

        deviceRepository.save(device);

        // Act
        boolean result = deviceRepository.existsByDeviceKey("DEVICE-TEST-001");

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByShelfIdShouldReturnTrueWhenShelfHasDevice() {
        // Arrange
        Device device = createDevice("DEVICE-TEST-002", shelf);

        deviceRepository.save(device);

        // Act
        boolean result = deviceRepository.existsByShelfId(shelf.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByDeviceKeyAndIdNotShouldIgnoreCurrentDevice() {
        // Arrange
        Device device = createDevice("DEVICE-TEST-003", shelf);

        Device saved = deviceRepository.save(device);

        // Act
        boolean result = deviceRepository
                .existsByDeviceKeyAndIdNot("DEVICE-TEST-003", saved.getId());

        // Assert
        assertFalse(result);
    }

    @Test
    void existsByShelfIdAndIdNotShouldIgnoreCurrentDevice() {
        // Arrange
        Device device = createDevice("DEVICE-TEST-004", shelf);

        Device saved = deviceRepository.save(device);

        // Act
        boolean result = deviceRepository.existsByShelfIdAndIdNot(shelf.getId(), saved.getId());

        // Assert
        assertFalse(result);
    }

    private Device createDevice(String deviceKey, Shelf shelf) {
        Device device = new Device();

        device.setDeviceKey(deviceKey);
        device.setStatus(DeviceStatus.INACTIVE);
        device.setShelf(shelf);

        return device;
    }
}