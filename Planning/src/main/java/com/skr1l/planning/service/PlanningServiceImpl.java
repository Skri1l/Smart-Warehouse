package com.skr1l.planning.service;

import com.skr1l.planning.exceptions.NoDataException;
import com.skr1l.planning.dto.BackendProductStockDto;
import com.skr1l.planning.dto.ExcelOrderItemDto;
import com.skr1l.planning.dto.PurchaseRecommendationDto;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanningServiceImpl implements PlanningService {

  @Override
  public List<PurchaseRecommendationDto> calculate(List<ExcelOrderItemDto> orders,
                                                   List<BackendProductStockDto> stocks) {

    Map<String, Integer> remainingBySku = new HashMap<>();

    List<PurchaseRecommendationDto> recommendations = new ArrayList<>();

    for (BackendProductStockDto stock : stocks) {
      remainingBySku.put(stock.productSKU(), stock.productRemains());
    }

    orders.sort(Comparator.comparing(ExcelOrderItemDto::dateOfNeed));

    for (ExcelOrderItemDto order : orders) {
      Integer available =  remainingBySku.get(order.productSKU());

      if (available == null) {
        throw new NoDataException("No data available for product " + order.productSKU());
      }

      if (available >= order.orderQuantity()){
        remainingBySku.put(order.productSKU(), available - order.orderQuantity());
      }
      else {
        int shortage = order.orderQuantity() - available;

        recommendations.add(new PurchaseRecommendationDto(
          order.productSKU(), shortage, order.dateOfNeed()));
        remainingBySku.put(order.productSKU(), 0);
      }
    }
    return recommendations;
  }
}
