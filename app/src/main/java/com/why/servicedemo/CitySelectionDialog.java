package com.why.servicedemo;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CitySelectionDialog {

    public interface OnCitySelectedListener {
        void onCitySelected(String cityName, String cityCode);
    }

    public static void showCitySelectionDialog(Context context, List<String> cityNames, final OnCitySelectedListener listener) {
        // 创建 AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("选择城市");

        // 创建 LayoutInflater
        LayoutInflater inflater = LayoutInflater.from(context);

        // 加载自定义布局
        View dialogView = inflater.inflate(R.layout.dialog_city_selection, null);
        builder.setView(dialogView);

        // 获取控件
        EditText searchEditText = dialogView.findViewById(R.id.searchEditText);
        ListView cityListView = dialogView.findViewById(R.id.cityListView);

//        // 对城市列表进行排序
//        Collections.sort(cityNames, new Comparator<String>() {
//            @Override
//            public int compare(String s1, String s2) {
//                return s1.compareToIgnoreCase(s2);
//            }
//        });

        // 创建适配器
        CityListAdapter adapter = new CityListAdapter(context, cityNames);
        cityListView.setAdapter(adapter);

        // 显示对话框
        final AlertDialog dialog = builder.create(); // 将 dialog 声明为 final
        dialog.show();

        // 设置 ListView 的点击事件
        cityListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCityName = adapter.getItem(position);
            CityCodeHelper cityCodeHelper = new CityCodeHelper(context);
            String selectedCityCode = cityCodeHelper.getCityCode(selectedCityName);

            if (listener != null) {
                listener.onCitySelected(selectedCityName, selectedCityCode);
            }

            // 关闭对话框
            dialog.dismiss();
        });


        // 设置搜索框的监听器
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 获取搜索文本
                String searchText = s.toString().toLowerCase();
                adapter.filter(searchText);  // 调用适配器的 filter 方法
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });


//        // 显示对话框
//        AlertDialog dialog = builder.create();
//        dialog.show();

    }
}
