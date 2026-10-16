package com.skr1l.planning;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Planning service", description = "planning orders managements API")
@Controller
@RestController
@RequestMapping("/planning")
@RequiredArgsConstructor
public class PlanningController {

  private final ExcelOrderReader reader;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<ExcelOrderItemDto> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

    return reader.read(file);
  }
}
