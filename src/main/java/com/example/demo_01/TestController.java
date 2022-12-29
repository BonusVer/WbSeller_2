package com.example.demo_01;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Controller
public class TestController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/home")
    public String uploadFile(@RequestParam("file") MultipartFile[] file) throws IOException {
            InputStream file01 = file[0].getInputStream();
            InputStream file02 = file[1].getInputStream();

        XSSFWorkbook wb1 = new XSSFWorkbook(file01);
        XSSFWorkbook wb2 = new XSSFWorkbook(file02);
        try {
            FileOutputStream outputStream = new FileOutputStream(
                    new File("/Users/bonusver/Documents/XXXXXXXXXXXX.xlsx"));
            FileOutputStream outputStream2 = new FileOutputStream(
                    new File("/Users/bonusver/Documents/YYYYYYYYYYYY.xlsx"));
            wb1.write(outputStream);
            wb2.write(outputStream2);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return "download";
    }
}
