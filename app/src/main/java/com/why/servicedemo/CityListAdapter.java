package com.why.servicedemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CityListAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> cityNames;
    private List<String> originalCityNames; // 保存原始城市列表

    public CityListAdapter(Context context, List<String> cityNames) {
        super(context, android.R.layout.simple_list_item_1, cityNames);
        this.context = context;
        this.cityNames = cityNames;
        this.originalCityNames = new ArrayList<>(cityNames); // 复制原始列表
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(cityNames.get(position));
        return convertView;
    }

    public void filter(String searchText) {
        cityNames.clear(); // 清空当前列表
        if (searchText.isEmpty()) {
            cityNames.addAll(originalCityNames); // 如果搜索为空，则恢复原始列表
        } else {
            searchText = searchText.toLowerCase();
            for (String cityName : originalCityNames) {
                if (cityName.toLowerCase().contains(searchText)) {
                    cityNames.add(cityName); // 添加过滤后的城市
                }
            }
        }
        notifyDataSetChanged(); // 通知适配器数据已更改
    }


    @Override
    public String getItem(int position) {
        return cityNames.get(position);
    }

    @Override
    public int getCount() {
        return cityNames.size();
    }
}
