package com.skr1l.planning;

import com.skr1l.planning.dto.BackendProductStockDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;

@Component
public class BackendStockClient {

  private final RestClient restClient;

  public BackendStockClient() {
    this.restClient = RestClient.builder()
      .baseUrl("http://localhost:8080")
      .build();
  }

  public List<BackendProductStockDto> getStocks(List<String> skus) {
    return restClient.get()
      .uri(builder -> builder
        .path("/stocks")
        .queryParam("sku", skus.toArray())
        .build())
      .retrieve()
      .body(new ParameterizedTypeReference<List<BackendProductStockDto>>() {});
  }
}
