package com.skr1l.backend.device.controller;

import com.skr1l.backend.vision.CameraImageService;
import com.skr1l.backend.vision.dto.CameraImageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devices")
public class DeviceImageController {

  private final CameraImageService cameraImageService;

  @PostMapping(value = "/{deviceId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CameraImageResponseDto> uploadImage(
    @PathVariable Long deviceId,
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime capturedAt,
    @RequestPart("file") MultipartFile file) {

    return ResponseEntity.ok(cameraImageService.process(deviceId, capturedAt, file));
  }
}
