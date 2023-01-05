package com.example.demo_01;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;

@RestController
public class TestController {

    @Autowired
    private ServiceCount serviceCount;

    @GetMapping("/")
    public ModelAndView home() {
        return new ModelAndView("home");
    }

    @PostMapping("/home")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile[] file)
            throws IOException {
        String filename = "Otchet.xlsx";
        InputStream file01 = file[0].getInputStream();
        InputStream file02 = file[1].getInputStream();
        try {
        XSSFWorkbook wb_price = new XSSFWorkbook(file01);
        XSSFWorkbook wb_helper = new XSSFWorkbook(file02);
        Sheet sheet_price = wb_price.getSheetAt(0);
        Sheet sheet_helper = wb_helper.getSheetAt(0);

        int[] z;
        for (Row row_helper: sheet_helper) {
            Cell cell_helper = row_helper.getCell(0);
            if (cell_helper.getCellType() == CellType.STRING) {
                continue;
            }
            for (Row row_price : sheet_price) {
                Cell cell_price = row_price.getCell(4);
                if (cell_price.getCellType() == CellType.STRING) {
                    continue;
                } else {
                    if (cell_helper.getNumericCellValue() == cell_price.getNumericCellValue()) {
                        Cell cell_price_cost = row_price.getCell(11); // цена в отчете
                        Cell cell_helper_sale = row_helper.getCell(1); //цена нужная
                        Cell cell_price_now_discount = row_price.getCell(13);//скидка действующая
                        Cell cell_price_sale = row_price.getCell(15); //запись скидки
                        Cell cell_price_price = row_price.getCell(12); // запись цены

                        z = serviceCount.sellerCore((int) cell_price_cost.getNumericCellValue(),
                                (int) cell_price_now_discount.getNumericCellValue(),
                                (int) cell_helper_sale.getNumericCellValue());
                        cell_price_sale.setCellValue(z[1]);
                        cell_price_price.setCellValue(z[0]);

                    }
                }
            }
        }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb_price.write(out);
            wb_price.close();
            wb_helper.close();
            file02.close();
            file01.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("ошибка номер 1 что то с эксель файлами" + e.getMessage());
        }

    }
}
