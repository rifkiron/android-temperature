package com.example.sensortemperature;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class MonitorService extends Service {
    private static final String CHANNEL_ID = "temperature_channel";
    private BatteryBroadcastReceiver batteryBroadcastReceiver;
    private HandlerThread handlerThread;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("TemperatureMonitorHandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        batteryBroadcastReceiver = new BatteryBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(1, getNotification("Monitoring started"));
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryBroadcastReceiver, filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryBroadcastReceiver);
        handlerThread.quitSafely();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification getNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Temperature Monitor")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Temperature Channel";
            String description = "Channel for temperature alerts";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private class BatteryBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.post(() -> {
                int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                float temperatureCelsius = temperature / 10.0f;

                // You can update the UI or take necessary actions here
                // For instance, show a notification if temperature exceeds a certain limit
            });
        }
    }
}
