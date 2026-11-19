package com.skr1l.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ExcelOrderItemDto(
  @NotBlank String orderNumber,
  @NotBlank String productSKU,
  String productName,
  @Positive int orderQuantity,
  @NotNull LocalDate dateOfNeed
) {
}
