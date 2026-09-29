package com.skr1l.backend.shelf.service;

import com.skr1l.backend.exception.rackExceptions.RackIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.rack.repository.RackRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.dto.ShelfRequestDto;
import com.skr1l.backend.shelf.dto.ShelfResponseDto;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ShelfServiceImpl implements ShelfService {

    private static final AppLogger log = LoggerFactory.getLogger(ShelfServiceImpl.class);

    private final ShelfRepository shelfRepository;

    private final RackRepository rackRepository;

    @Override
    @Transactional
    public Long createShelf(ShelfRequestDto dto) {
        log.info("createShelf");

        Objects.requireNonNull(dto, "dto is null");

        Rack rack = getRackEntityById(dto.rackId());

        Shelf shelf = new Shelf();
        shelf.setLabel(dto.label());
        shelf.setCapacity(dto.capacity());
        shelf.setRack(rack);
        shelf.setStatus(ShelfStatus.ACTIVE);

        shelfRepository.save(shelf);

        return shelf.getId();
    }

    @Override
    public ShelfResponseDto getShelfById(Long id) {
        log.info("getShelfById");

        Objects.requireNonNull(id, "id is null");

        Shelf shelf = getShelfEntityById(id);

        return toResponseDto(shelf);
    }

    @Override
    public List<ShelfResponseDto> getAllShelves() {
        log.info("getAllShelves");

        return shelfRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public ShelfResponseDto updateShelf(Long id, ShelfRequestDto dto) {
        log.info("updateShelf");

        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        Shelf shelf = getShelfEntityById(id);

        Rack rack = getRackEntityById(dto.rackId());

        shelf.setLabel(dto.label());
        shelf.setCapacity(dto.capacity());
        shelf.setRack(rack);

        return toResponseDto(shelf);
    }

    @Override
    @Transactional
    public void deleteShelfById(Long id) {
        log.info("deleteShelfById");

        Objects.requireNonNull(id, "id is null");

        Shelf shelf = getShelfEntityById(id);

        shelfRepository.delete(shelf);
    }

    @Override
    @Transactional
    public void activateShelf(Long id) {
        log.info("activateShelf");

        Shelf shelf = getShelfEntityById(id);

        shelf.setStatus(ShelfStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void deactivateShelf(Long id) {
        log.info("deactivateShelf");

        Shelf shelf = getShelfEntityById(id);

        shelf.setStatus(ShelfStatus.INACTIVE);
    }

    private Shelf getShelfEntityById(Long id) {
        log.info("getShelfEntityById");

        return shelfRepository.findById(id)
                .orElseThrow(() -> new ShelfIsNotFoundException(
                        "Shelf with id " + id + " is not found"));
    }

    private Rack getRackEntityById(Long id) {
        log.info("getRackEntityById");

        return rackRepository.findById(id)
                .orElseThrow(() -> new RackIsNotFoundException(
                                "Rack with id " + id + " is not found"));
    }

    private ShelfResponseDto toResponseDto(Shelf shelf) {
        log.info("shelfToResponseDto");

        return new ShelfResponseDto(
                shelf.getId(),
                shelf.getLabel(),
                shelf.getCapacity(),
                shelf.getStatus(),
                shelf.getRack().getId());
    }
}