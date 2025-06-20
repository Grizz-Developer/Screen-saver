package com.why.servicedemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.qweather.sdk.QWeather;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 100; // 通知权限请求码
    private static final int LOCATION_PERMISSION_CODE = 101;  // 定位权限请求码
    private TextView timeTextView, dateTextView, lunarTextView, weatherTextView,batteryTextView;
    private Handler handler = new Handler();
    private Runnable timeRunnable, positionRunnable,weatherRunnable;
    private RelativeLayout.LayoutParams timeParams;
    private static final String HEFENG_API_KEY = ""; // 替换 API Key
    private static final String CITY_ID = "101280601"; // 城市 ID
    private final OkHttpClient client = new OkHttpClient();
    private boolean isMovingUp = true; // 初始方向为向上
    private int moveDistance = 50;    //每次平移的距离(像素)，可以根据需要调整
    private BroadcastReceiver batteryReceiver;
    private CityCodeHelper cityCodeHelper;
    private String currentCityId; //当前城市ID
    private static final String PREF_NAME = "WeatherAppPrefs";
    private static final String CITY_CODE_KEY = "cityCode";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 移除标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // 初始化视图
        initview();

        // 初始化 CityCodeHelper
        cityCodeHelper = new CityCodeHelper(this);

        // 获取保存的城市代码
        String savedCityCode = getSavedCityCode();

        if (savedCityCode != null) {
            // 更新天气信息
            updateWeatherInfo(savedCityCode);
        } else {
            // 如果没有保存的城市代码，显示一个默认城市的天气信息
            updateWeatherInfo("101010100"); // 默认北京
        }

        //设置weatherTextView的点击事件
        weatherTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> cityNames = cityCodeHelper.getCityNames();
                CitySelectionDialog.showCitySelectionDialog(MainActivity.this, cityNames, new CitySelectionDialog.OnCitySelectedListener() {
                    @Override
                    public void onCitySelected(String cityName, String cityCode) {
                        // 保存选择的城市代码
                        saveCityCode(cityCode);
                        // 更新天气信息 (根据新的城市代码获取)
                        currentCityId = cityCode;
                        updateWeatherInfo(cityCode);
                    }
                });
            }
        });

        // 初始化和风天气
        QWeather.getInstance(MainActivity.this, "{kj6x87m9b8.re.qweatherapi.com}") // 初始化服务地址
                .setLogEnable(true);  // 启用调试日志（生产环境建议设置为 false）

        // 请求通知权限
        requestNotificationPermission();

        // 请求定位权限
        requestLocationPermission();

        // 启动服务
        Intent intent = new Intent(this, MyService.class);
        startService(intent);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 每分钟更新时间
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                handler.postDelayed(this, 1000); // 每秒更新一次，显示秒
            }
        };
        handler.post(timeRunnable);

        // 每 1 分钟移动位置
        positionRunnable = new Runnable() {
            @Override
            public void run() {
                moveTimeTextView();
                handler.postDelayed(this, 1 * 60 * 1000); // 1 分钟
            }
        };
        handler.post(positionRunnable);

        // 每 10 分钟更新天气
        weatherRunnable = new Runnable() {
            @Override
            public void run() {
                updateWeather(currentCityId);
                handler.postDelayed(this, 10 * 60 * 1000); // 10 分钟
            }
        };
        handler.post(weatherRunnable);

        // 注册电量广播接收器
        registerBatteryReceiver();
    }

    // 请求定位权限
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 权限未授予，申请权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            // 权限已授予，获取城市ID
//            getCityIdByLocation();
        }
    }

    private void requestNotificationPermission() {
        // 只在 Android 13 及以上版本请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以发送通知
                Log.d(TAG, "Notification permission already granted");
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // 向用户解释为什么需要通知权限
                showNotificationPermissionRationale();
            } else {
                // 请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        } else {
            // Android 12 及以下版本不需要显式申请通知权限
            Log.d(TAG, "Notification permission not required on this Android version");
        }
    }

    // 显示权限请求理由的对话框
    private void showNotificationPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("通知权限请求")
                .setMessage("此应用程序需要通知权限才能显示重要的更新和消息。")
                .setPositiveButton("确定", (dialog, which) -> {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // 权限已授予，获取城市ID
//                getCityIdByLocation();
            } else {
                // 权限被拒绝，显示默认信息或提示用户
                Log.w(TAG, "Location permission denied");
                runOnUiThread(() -> weatherTextView.setText("Location permission denied"));
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了权限，可以发送通知
                Log.d(TAG, "Notification permission granted by user");
            } else {
                // 用户拒绝了权限，提供反馈或禁用依赖于通知的功能
                Log.d(TAG, "Notification permission denied by user");
                // 可以向用户显示一条消息，解释说他们将无法收到通知
            }
        }
    }


    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        String currentDate = dateFormat.format(new Date());

        // 获取星期几
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String weekDay = getWeekDayString(dayOfWeek);

//        Calendar calendar = Calendar.getInstance();
//        LunarCalendarUtils.Lunar lunar = LunarCalendarUtils.solarToLunar(calendar);
//        String lunarDate = lunar.toString();
        timeTextView.setText(currentTime);
        dateTextView.setText(currentDate + " " + weekDay);
//        lunarTextView.setText(lunarDate);
    }

    // 将数字转换为中文星期几
    private String getWeekDayString(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "星期日";
            case Calendar.MONDAY:
                return "星期一";
            case Calendar.TUESDAY:
                return "星期二";
            case Calendar.WEDNESDAY:
                return "星期三";
            case Calendar.THURSDAY:
                return "星期四";
            case Calendar.FRIDAY:
                return "星期五";
            case Calendar.SATURDAY:
                return "星期六";
            default:
                return "";
        }
    }

    private void moveTimeTextView() {
        int margin = 25; // 边缘安全距离
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int textHeight = timeTextView.getHeight();

        // 获取当前 topMargin
        int currentTopMargin = timeParams.topMargin;

        // 计算新的 topMargin
        int newTopMargin = currentTopMargin + (isMovingUp ? -moveDistance : moveDistance);

        // 边界检查
        if (newTopMargin < margin) {
            // 如果超出上边界，则反向移动
            newTopMargin = margin;
            isMovingUp = false;
        } else if (newTopMargin > screenHeight - textHeight - margin) {
            // 如果超出下边界，则反向移动
            newTopMargin = screenHeight - textHeight - margin;
            isMovingUp = true;
        }

        timeParams.topMargin = newTopMargin;
        timeTextView.setLayoutParams(timeParams);
    }


    private void updateWeather(String cityId) {
        String url = "https://kj6x87m9b8.re.qweatherapi.com/v7/weather/now?location=" + cityId + "&key=" + HEFENG_API_KEY;
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch weather data: " + e.getMessage());
                // 在UI线程更新UI，显示错误信息
                runOnUiThread(() -> weatherTextView.setText("加载天气失败"));
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "Weather data: " + jsonData);
                    Gson gson = new Gson();
                    WeatherResponse weatherResponse = gson.fromJson(jsonData, WeatherResponse.class);
                    // 检查返回码，确保请求成功
                    if ("200".equals(weatherResponse.code)) {
                        String temperature = weatherResponse.now.temperature;
                        String condition = weatherResponse.now.condition;
                        // 获取城市名称（这里需要从cityCodeHelper中根据cityId获取城市名，可能需要修改CityCodeHelper）
                        String cityName = getCityNameByCityCode(cityId);
                        if (cityName == null){
                            cityName = "未知";
                        }
                        // 在UI线程更新UI
                        String finalCityName = cityName;
                        runOnUiThread(() -> weatherTextView.setText(finalCityName + "  " + condition + "  " + temperature + "℃" + " "));
                    } else {
                        // 处理 API 返回的错误码
                        Log.e(TAG, "API Error: " + weatherResponse.code);
                        runOnUiThread(() -> weatherTextView.setText("Weather data unavailable"));
                    }
                } else {
                    Log.e(TAG, "Response not successful: " + response.code());
                    runOnUiThread(() -> weatherTextView.setText("加载天气失败"));
                }
            }
        });
    }

    // 根据城市ID获取城市名称
    private String getCityNameByCityCode(String cityCode) {
        //TODO：修改CityCodeHelper，使其能够通过cityCode获取cityName
        return cityCodeHelper.getCityCodeMap().entrySet().stream().filter(entry -> cityCode.equals(entry.getValue()))
                .map(entry -> entry.getKey()).findFirst().orElse(null);
    }

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float)scale;
                batteryTextView.setText("电量：" + String.format("%.0f", batteryPct) + "%");
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }


    private void saveCityCode(String cityCode) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CITY_CODE_KEY, cityCode);
        editor.apply(); // 异步保存
    }
    private String getSavedCityCode() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getString(CITY_CODE_KEY, null); // 默认值为 null
    }
    private void updateWeatherInfo(String cityCode) {
        // 更新当前城市ID
        currentCityId = cityCode;
        // 立即更新天气信息
        updateWeather(cityCode);
    }


    private void initview() {
        timeTextView = findViewById(R.id.timeTextView);
        dateTextView = findViewById(R.id.dateTextView);
//        lunarTextView = findViewById(R.id.lunarTextView);
        weatherTextView = findViewById(R.id.weatherTextView);
        batteryTextView = findViewById(R.id.batteryTextView);

        // 初始化 timeParams
        timeParams = (RelativeLayout.LayoutParams) timeTextView.getLayoutParams();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 移除屏幕常亮标志
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 移除 Handler 的回调，防止内存泄漏
        handler.removeCallbacks(timeRunnable);
        handler.removeCallbacks(positionRunnable);
        handler.removeCallbacks(weatherRunnable);

        // 取消注册电量广播接收器
        unregisterReceiver(batteryReceiver);
    }

}
