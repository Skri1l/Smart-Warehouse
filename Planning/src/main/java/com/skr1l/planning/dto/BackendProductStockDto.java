package com.skr1l.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record BackendProductStockDto(
  @NotBlank String productSKU,
  @PositiveOrZero int productRemains
) {
}
