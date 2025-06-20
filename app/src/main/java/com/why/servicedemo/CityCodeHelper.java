package com.why.servicedemo;

import android.content.Context;
import android.content.res.AssetManager;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CityCodeHelper {

    private Map<String, String> cityCodeMap = new HashMap<>(); // 存储城市名称和代码的映射
    private List<String> cityNames = new ArrayList<>();        // 存储城市名称列表

    public CityCodeHelper(Context context) {
        loadCityCodesFromAssets(context);
    }

    private void loadCityCodesFromAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open("China-City-List-latest.csv");
             CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String[] nextLine;
            reader.readNext(); // 跳过标题行
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 2) { // 确保有足够多的列
                    String cityNameZH = nextLine[2]; // Location_Name_ZH 列
                    String locationID = nextLine[0];  // Location_ID 列

                    cityCodeMap.put(cityNameZH, locationID);
                    cityNames.add(cityNameZH);        // 添加到城市列表
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public String getCityCode(String cityName) {
        return cityCodeMap.get(cityName);
    }

    public Map<String, String> getCityCodeMap() {
        return cityCodeMap;
    }

    public List<String> getCityNames() {
        return cityNames;
    }
}
