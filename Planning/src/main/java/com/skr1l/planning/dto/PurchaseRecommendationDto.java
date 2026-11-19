package com.skr1l.planning.dto;

import java.time.LocalDate;

public record PurchaseRecommendationDto(
  String productSKU,
  int quantityToBuy,
  LocalDate dateOfNeed
)
{}
