package com.example.demo_01;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.util.Objects;

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
            throws IOException, IllegalStateException {
        String filename = "Otchet.xlsx";
        String message = "";

        try {
            if (ExcelHelper.hasExcelFormat(file[0]) | ExcelHelper.hasExcelFormat(file[1])) {
                message = "Проверьте загружаемые файлы. Программа принимает только эксель файлы.";
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
            }

            InputStream file01 = file[0].getInputStream();
            InputStream file02 = file[1].getInputStream();


        XSSFWorkbook wb_price = new XSSFWorkbook(file01);
        XSSFWorkbook wb_helper = new XSSFWorkbook(file02);
        Sheet sheet_price = wb_price.getSheetAt(0);
        Sheet sheet_helper = wb_helper.getSheetAt(0);

        XSSFCellStyle cellStyle = wb_price.createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());

        String h_s = sheet_price.getSheetName();
        String h_h = "Общий отчет";
        if (!Objects.equals(h_s, h_h)) {
            wb_price.close();
            wb_helper.close();
            file02.close();
            file01.close();
            message = "Отчет ВБ не загружен в первый слот или поврежден." +
                    "Возможно перепутаны загружаемые файлы";
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ResponseMessage(message));
        }

        int[] z;
        int flag = 0;
        int f_row = 1;
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
                        int cost = (int) cell_price_cost.getNumericCellValue();
                        int sale = (int) cell_price_now_discount.getNumericCellValue();
                        z = serviceCount.sellerCore((int) cell_price_cost.getNumericCellValue(),
                                (int) cell_price_now_discount.getNumericCellValue(),
                                (int) cell_helper_sale.getNumericCellValue());

                        if (flag == 0 && z[2] != 0) {
                            Sheet sheet_2 = wb_price.createSheet("Примечание");
                            Row row_2 = sheet_2.createRow(0);
                            Cell cell_01 = row_2.createCell(0);
                            cell_01.setCellValue("Артикул WB");
                            Cell cell_02 = row_2.createCell(1);
                            cell_02.setCellValue("Примечание");
                            flag = 1;
                        }

                        if (z[2] != 0) {
                            Sheet sheet_h = wb_price.getSheet("Примечание");
                            sheet_h.autoSizeColumn(1);
                            Row row_h = sheet_h.createRow(f_row);
                            f_row += 1;
                            Cell h_0 = row_h.createCell(0);
                            h_0.setCellValue((int) cell_price.getNumericCellValue());
                            switch (z[2]) {
                                case 1:
                                    Cell h_0_2 = row_h.createCell(1);
                                    h_0_2.setCellValue("Скорее всего данный товар новый на сайте. " +
                                            "Скидка не может быть выше 60% по правилам маркетплейса. " +
                                            "Цена товара до скидки была снижена более 20%");
                                    break;
                                case 2:
                                    Cell h_1_2 = row_h.createCell(1);
                                    h_1_2.setCellValue("Цена до скидки была снижена более 20%");
                                    break;

                                case 3:
                                    Cell h_2_2 = row_h.createCell(1);
                                    h_2_2.setCellStyle(cellStyle);
                                    h_2_2.setCellValue("Цена товара не изменена." +
                                            "Заявленная цена не соответствует правилам сайта. " +
                                            "Укажите более низкую цену");
                                    break;

                                case 4:
                                    Cell h_3_2 = row_h.createCell(1);
                                    h_3_2.setCellStyle(cellStyle);
                                    h_3_2.setCellValue("Цена товара не изенена. " +
                                            "Товару указана цена меньше 50 руб.");
                                    break;
                            }
                        }

                        if (z[2] == 3 | z[2] == 4) {
                            continue;
                        }
                        if (cost == z[0] & sale == z[1]) {continue;}
                        if (cost == z[0]) {
                            cell_price_sale.setCellValue(z[1]);
                            continue;
                        }
                        if (sale == z[1]) {
                            cell_price_price.setCellValue(z[0]);
                            continue;
                        }
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
        } catch (IllegalStateException e) {
            message = "Ошибка в фале.";
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ResponseMessage(message));
        } catch (NullPointerException e) {
            message = "Ошибка в загруженном файле. Возможно пропущена цена у артикула.";
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ResponseMessage(message));
        }
    }
}
