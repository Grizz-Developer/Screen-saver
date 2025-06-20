package com.why.servicedemo;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("now")
    public Now now;

    @SerializedName("code")
    public String code;

    public class Now {
        @SerializedName("temp")
        public String temperature; // 温度

        @SerializedName("text")
        public String condition;   // 天气状况描述

        @SerializedName("name")  // 城市名称字段
        public String name;

        @SerializedName("icon")  // 城市名称字段
        public String icon;
    }
}
