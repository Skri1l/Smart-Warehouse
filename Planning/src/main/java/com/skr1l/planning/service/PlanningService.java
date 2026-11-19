package com.skr1l.planning.service;

import com.skr1l.planning.dto.BackendProductStockDto;
import com.skr1l.planning.dto.ExcelOrderItemDto;
import com.skr1l.planning.dto.PurchaseRecommendationDto;

import java.util.List;

public interface PlanningService {

  List<PurchaseRecommendationDto> calculate (List<ExcelOrderItemDto> orders,
                                             List<BackendProductStockDto> stocks);
}
