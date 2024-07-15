package com.example.sensortemperature;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "temperature_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;

    private TextView textTemperature;
    private TextView textRealTimeBatteryTemp;

    private EditText editMaxTemperature;
    private Button btnStart, btnStop, btnBackground;

    private float maxTemperature;
    private boolean isMonitoring;

    private RecyclerView recyclerView;
    private TemperatureAdapter adapter;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private TextView textStatus;

    private BatteryBroadcastReceiver batteryBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        Cursor cursor = getTemperatureData();
        adapter = new TemperatureAdapter(this, cursor);
        recyclerView.setAdapter(adapter);

        createNotificationChannel();

        textStatus = findViewById(R.id.textStatus);
        textTemperature = findViewById(R.id.textTemperature);
        textRealTimeBatteryTemp = findViewById(R.id.textRealTimeBatteryTemp);
        editMaxTemperature = findViewById(R.id.editMaxTemperature);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> startTemperatureMonitoring());
        btnStop.setOnClickListener(v -> stopTemperatureMonitoring());


        batteryBroadcastReceiver = new BatteryBroadcastReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        checkAutoStartPermission();
    }

    private void checkAutoStartPermission() {
        if (!isAutoStartEnabled()) {
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            switch (manufacturer) {
                case "xiaomi":
                    autoStartIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                    break;
                case "oppo":
                    autoStartIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                    break;
                case "vivo":
                    autoStartIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager");
                    break;
                case "letv":
                    autoStartIntent("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity");
                    break;
                case "honor":
                    autoStartIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
                    break;
                default:
                    openDefaultSettings();
                    break;
            }
        }
    }

    private boolean isAutoStartEnabled() {
        try {
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            switch (manufacturer) {
                case "xiaomi":
                    return isXiaomiAutoStartEnabled();

                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isXiaomiAutoStartEnabled() {
        try {
            PackageManager packageManager = getPackageManager();
            ComponentName componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            int state = packageManager.getComponentEnabledSetting(componentName);
            return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private void showManualAutoStartInstructions() {
        new AlertDialog.Builder(this)
                .setTitle("Auto-Start Permission")
                .setMessage("Please enable auto-start permission for this app in your device settings.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void autoStartIntent(String packageName, String className) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
            if (isIntentAvailable(intent)) {
                startActivity(intent);
            } else {
                showManualAutoStartInstructions();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showManualAutoStartInstructions();
        }
    }

    private boolean isIntentAvailable(Intent intent) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    private void openDefaultSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class BatteryBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            float temperatureCelsius = temperature / 10.0f;

            textRealTimeBatteryTemp.setText(String.format("Real-time Temperature: %.1f °C", temperatureCelsius));

            if (isMonitoring) {
                textTemperature.setText(String.format("Temperature: %.1f °C", temperatureCelsius));

                if (temperatureCelsius > maxTemperature) {
                    saveTemperatureToDatabase(temperatureCelsius);
                    showTemperatureNotification(temperatureCelsius);
                }
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Temperature Channel";
            String description = "Channel for temperature alerts";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startTemperatureMonitoring() {
        String maxTempStr = editMaxTemperature.getText().toString();
        if (!maxTempStr.isEmpty()) {
            maxTemperature = Float.parseFloat(maxTempStr);
            isMonitoring = true;
            textStatus.setText("Status: Monitoring Started");
            editMaxTemperature.setEnabled(false);

            Intent serviceIntent = new Intent(this, MonitorService.class);
            startService(serviceIntent);

            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryBroadcastReceiver, filter);

            startBackgroundService();
        }
    }

    private void stopTemperatureMonitoring() {
        isMonitoring = false;
        textStatus.setText("Status: Monitoring Stopped");
        editMaxTemperature.setEnabled(true);

        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);

        if (batteryBroadcastReceiver != null) {
            try {
                unregisterReceiver(batteryBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, MonitorService.class);
        startService(serviceIntent);
    }

    private void saveTemperatureToDatabase(float temperatureValue) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TEMPERATURE, temperatureValue);

        long currentTimeMillis = System.currentTimeMillis();
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, currentTimeMillis);

        long newRowId = db.insert(DatabaseHelper.TABLE_TEMPERATURE, null, values);

        Cursor cursor = getTemperatureData();
        adapter.swapCursor(cursor);
    }

    private Cursor getTemperatureData() {
        return db.query(DatabaseHelper.TABLE_TEMPERATURE, null, null, null, null, null, null);
    }

    private void showTemperatureNotification(float currentTemperature) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String notificationText = String.format("Temperature exceeds %.1f °C", maxTemperature);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("High Battery Temperature Warning")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (batteryBroadcastReceiver != null) {
            try {
                unregisterReceiver(batteryBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan
            } else {
                // Izin ditolak
            }
        }
    }
}
