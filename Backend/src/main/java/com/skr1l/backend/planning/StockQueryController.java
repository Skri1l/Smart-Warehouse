package com.skr1l.backend.planning;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockQueryController {

  private final StockQueryService stockQueryService;

  @GetMapping
  public List<ProductStockResponseDto> getStocks(@RequestParam("sku") List<String> skus) {

    return stockQueryService.getStocks(skus);
  }
}
