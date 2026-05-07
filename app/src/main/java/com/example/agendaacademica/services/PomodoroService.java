package com.example.agendaacademica.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.agendaacademica.R;

public class PomodoroService extends Service {
    private final IBinder binder = new PomodoroBinder();
    private CountDownTimer timer;
    private long timeLeftInMillis = 25 * 60 * 1000; // 25 minutos
    private boolean isRunning = false;

    public class PomodoroBinder extends Binder {
        public PomodoroService getService() { return PomodoroService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, getNotification("Pomodoro en curso"));
        return START_NOT_STICKY;
    }

    public void startTimer(long durationInMillis) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(durationInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
            }
            public void onFinish() {
                isRunning = false;
                stopForeground(true);
                stopSelf();
            }
        }.start();
        isRunning = true;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("pomodoro_channel", "Pomodoro", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification getNotification(String content) {
        return new NotificationCompat.Builder(this, "pomodoro_channel")
                .setContentTitle("Agenda Académica")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }
}
