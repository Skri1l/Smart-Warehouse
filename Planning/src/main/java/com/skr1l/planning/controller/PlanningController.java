package com.skr1l.planning.controller;

import com.skr1l.planning.BackendStockClient;
import com.skr1l.planning.ExcelOrderReader;
import com.skr1l.planning.dto.BackendProductStockDto;
import com.skr1l.planning.dto.ExcelOrderItemDto;
import com.skr1l.planning.dto.PurchaseRecommendationDto;
import com.skr1l.planning.service.PlanningService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;

import java.io.IOException;
import java.util.List;

@Tag(name = "Planning service", description = "planning orders managements API")
@RestController
@RequestMapping("/planning")
@RequiredArgsConstructor
public class PlanningController {

  private final ExcelOrderReader reader;

  private final PlanningService service;

  private final BackendStockClient client;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<PurchaseRecommendationDto> calculateRemains (@RequestPart("file") MultipartFile file)
    throws IOException {

    List<ExcelOrderItemDto> orders = reader.read(file);

    List<String> skus = orders.stream()
      .map(ExcelOrderItemDto::productSKU)
      .distinct()
      .toList();

    List<BackendProductStockDto> stocks = client.getStocks(skus);

    return service.calculate(orders, stocks);
  }
}
