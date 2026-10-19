package com.skr1l.backend.planning;

import com.skr1l.backend.shelfStock.repository.ShelfStockRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockQueryService {

  private final ShelfStockRepository shelfStockRepository;

  @Transactional(readOnly = true)
  public List<ProductStockResponseDto> getStocks(List<String> skus){
    if(skus == null || skus.isEmpty()
      || skus.stream().anyMatch(sku -> sku == null || sku.isBlank())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "skus is null or empty");
    }

    List<ProductStockResponseDto> stocks = shelfStockRepository.findStockTotalsBySkus(skus).stream()
      .map(row -> new ProductStockResponseDto(
        row.getProductSKU(), Math.toIntExact(row.getTotalQuantity())))
      .toList();


    Set<String> missing = new HashSet<>(skus);
    stocks.forEach(stock -> missing.remove(stock.productSKU()));

    if(!missing.isEmpty()){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "skus is empty");
    }

    return stocks;
  }
}
