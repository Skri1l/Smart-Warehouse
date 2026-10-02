package com.skr1l.backend.device;

import com.skr1l.backend.device.controller.DeviceController;
import com.skr1l.backend.device.dto.DeviceRequestDto;
import com.skr1l.backend.device.dto.DeviceResponseDto;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.device.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new DeviceController(deviceService))
                .build();
    }

    @Test
    void getDeviceByIdShouldReturnDevice() throws Exception {
        when(deviceService.getDeviceById(30L))
                .thenReturn(new DeviceResponseDto(
                        30L,
                        "CAM-001",
                        DeviceStatus.INACTIVE,
                        null,
                        20L
                ));

        mockMvc.perform(get("/devices/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.deviceKey").value("CAM-001"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.shelfId").value(20));
    }

    @Test
    void getAllDevicesShouldReturnDevices() throws Exception {
        when(deviceService.getAllDevices())
                .thenReturn(List.of(new DeviceResponseDto(
                        30L, "CAM-001", DeviceStatus.INACTIVE, null, 20L)));

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceKey")
                        .value("CAM-001"));
    }

    @Test
    void createDeviceShouldReturnCreatedAndId() throws Exception {
        when(deviceService.createDevice(any(DeviceRequestDto.class)))
                .thenReturn(30L);

        String json = """
                {
                    "deviceKey": "CAM-001",
                    "shelfId": 20
                }
                """;

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("30"));
    }

    @Test
    void updateDeviceShouldReturnUpdatedDevice() throws Exception {
        when(deviceService.updateDevice(eq(30L), any(DeviceRequestDto.class)))
                .thenReturn(new DeviceResponseDto(30L, "CAM-002",
                        DeviceStatus.INACTIVE, null, 20L));

        String json = """
                {
                    "deviceKey": "CAM-002",
                    "shelfId": 20
                }
                """;

        mockMvc.perform(put("/devices/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceKey")
                        .value("CAM-002"));
    }

    @Test
    void activateDeviceShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/devices/30/activate"))
                .andExpect(status().isNoContent());

        verify(deviceService).activateDevice(30L);
    }

    @Test
    void deactivateDeviceShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/devices/30/deactivate"))
                .andExpect(status().isNoContent());

        verify(deviceService).deactivateDevice(30L);
    }

    @Test
    void heartbeatShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/devices/30/heartbeat"))
                .andExpect(status().isNoContent());

        verify(deviceService).heartbeat(30L);
    }

    @Test
    void deleteDeviceShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/devices/30"))
                .andExpect(status().isNoContent());

        verify(deviceService).deleteDeviceById(30L);
    }
}