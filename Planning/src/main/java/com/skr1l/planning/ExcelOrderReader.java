package com.skr1l.planning;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelOrderReader {

  public List<ExcelOrderItemDto> read(MultipartFile file) throws IOException {

    List<ExcelOrderItemDto> orders = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
         XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);

        if (row == null) {
          continue;
        }

        orders.add(parseRow(row));
      }
      return orders;
    }
  }

  private ExcelOrderItemDto parseRow(Row row) {

    String orderNumber = row.getCell(0).getStringCellValue();
    String productSKU = row.getCell(1).getStringCellValue();
    String productName = row.getCell(2).getStringCellValue();
    int orderQuantity = Integer.parseInt(row.getCell(3).getStringCellValue());
    LocalDate dateOfNeed = LocalDate.parse(row.getCell(4).getStringCellValue());

    return new ExcelOrderItemDto(orderNumber, productSKU, productName, orderQuantity, dateOfNeed);
  }
}
