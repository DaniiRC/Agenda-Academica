package com.example.edusync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Receptor de radiodifusiÃ³n (BroadcastReceiver) encargado de gestionar las alarmas del sistema.
 * Cuando se activa una alarma programada, este receptor crea y muestra una notificaciÃ³n local
 * en la barra de estado, permitiendo la redirecciÃ³n directa al detalle del evento.
 */
public class NotificacionReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "CanalEduSync";

    /**
     * MÃ©todo ejecutado cuando se recibe el broadcast de la alarma.
     * Recupera los parÃ¡metros del evento y construye la notificaciÃ³n visual.
     * 
     * @param context El contexto en el que se ejecuta el receptor.
     * @param intent  El intent que contiene los datos del recordatorio (tÃ­tulo, contenido, ID).
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificacionReceiver", "Â¡ALARMA RECIBIDA! Procesando notificaciÃ³n...");

        String titulo = intent.getStringExtra("TITULO");
        String contenido = intent.getStringExtra("CONTENIDO");
        long idEvento = intent.getLongExtra("EVENTO_ID", -1L);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        configurarCanalNotificaciones(notificationManager);

        Intent detalleIntent = new Intent(context, com.example.edusync.activities.DetalleEventoActivity.class);
        detalleIntent.putExtra("EVENTO_ID", idEvento);
        detalleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) idEvento,
                detalleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_school)
                .setContentTitle(titulo != null ? titulo : "EduSync")
                .setContentText(contenido != null ? contenido : "Tienes una tarea pendiente ahora")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) idEvento, builder.build());
            Log.d("NotificacionReceiver", "NotificaciÃ³n enviada para evento ID: " + idEvento);
        }
    }

    /**
     * Configura el canal de notificaciones requerido para Android Oreo (API 26) y superiores.
     * 
     * @param manager El administrador de notificaciones del sistema.
     */
    private void configurarCanalNotificaciones(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Avisos de Eventos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones para recordatorios de la EduSync");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
    }
}
