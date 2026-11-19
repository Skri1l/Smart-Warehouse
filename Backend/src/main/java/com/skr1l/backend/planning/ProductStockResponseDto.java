package com.skr1l.backend.planning;

public record ProductStockResponseDto(
  String productSKU,
  int productRemains
) {
}
