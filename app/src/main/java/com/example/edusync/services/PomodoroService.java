package com.example.edusync.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.edusync.R;

/**
 * Servicio en primer plano que gestiona el temporizador Pomodoro.
 * Permite mantener el contador activo aunque la aplicaciÃ³n estÃ© en segundo plano,
 * mostrando una notificaciÃ³n persistente mientras el temporizador estÃ¡ en ejecuciÃ³n.
 */
public class PomodoroService extends Service {
    private final IBinder binder = new PomodoroBinder();
    private CountDownTimer timer;
    private long timeLeftInMillis = 25 * 60 * 1000; // Tiempo inicial: 25 minutos por defecto
    private boolean isRunning = false;

    /**
     * Binder que expone la instancia del servicio a la actividad vinculada.
     */
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

    /**
     * Inicia el temporizador con la duraciÃ³n indicada.
     * Si ya existe un temporizador activo, se cancela antes de iniciar el nuevo.
     *
     * @param durationInMillis DuraciÃ³n del temporizador en milisegundos.
     */
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
                .setContentTitle("EduSync")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }
}
