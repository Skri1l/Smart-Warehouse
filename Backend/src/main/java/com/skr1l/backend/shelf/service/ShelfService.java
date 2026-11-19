package com.skr1l.backend.shelf.service;

import com.skr1l.backend.shelf.dto.ShelfRequestDto;
import com.skr1l.backend.shelf.dto.ShelfResponseDto;
import java.util.List;

public interface ShelfService {

    Long createShelf(ShelfRequestDto dto);

    ShelfResponseDto getShelfById(Long id);

    List<ShelfResponseDto> getAllShelves();

    ShelfResponseDto updateShelf(Long id, ShelfRequestDto dto);

    void deleteShelfById(Long id);

    void activateShelf(Long id);

    void deactivateShelf(Long id);
}