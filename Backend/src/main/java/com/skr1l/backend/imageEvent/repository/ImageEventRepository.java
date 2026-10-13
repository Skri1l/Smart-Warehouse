package com.skr1l.backend.imageEvent.repository;

import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImageEventRepository extends JpaRepository<ImageEvent, Long> {

    List<ImageEvent> findByDeviceId(Long deviceId);

    List<ImageEvent> findByShelfId(Long shelfId);

    List<ImageEvent> findByImageProcessingStatus(ImageProcessingStatus imageProcessingStatus);

    List<ImageEvent> findByCapturedAtBetween(LocalDateTime from, LocalDateTime to);

    boolean existsByShelfIdAndImageProcessingStatusAndCapturedAtGreaterThanEqual(
        Long shelfId, ImageProcessingStatus status, LocalDateTime capturedAt);
}
