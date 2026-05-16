package com.example.edusync.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.edusync.activities.MainActivity;
import com.example.edusync.R;

public class EduSyncWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Establecer el intent para el servicio que proporciona la lista
        Intent serviceIntent = new Intent(context, EduSyncWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_edusync);
        views.setRemoteAdapter(R.id.widgetList, serviceIntent);
        views.setEmptyView(R.id.widgetList, R.id.widgetEmptyView);

        // Colores de iconos (Safe for RemoteViews)
        int primaryColor = context.getResources().getColor(R.color.primary, null);
        int subTextColor = 0xFF6B7280; // Gris suave (text_sub)
        views.setInt(R.id.widgetIconCalendar, "setColorFilter", primaryColor);
        views.setInt(R.id.widgetRefresh, "setColorFilter", subTextColor);

        // Click en el título abre la app
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetTitle, pendingIntent);

        // Click en refrescar
        Intent refreshIntent = new Intent(context, EduSyncWidget.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRefresh, refreshPendingIntent);

        // Template para clics en los items de la lista (abre el detalle)
        Intent detailIntent = new Intent(context, com.example.edusync.activities.DetalleEventoActivity.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, detailIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widgetList, clickPendingIntent);

        // Notificar que los datos han cambiado para que el ListView se refresque
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetList);

        // Actualizar el widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Ejecutado cuando se crea el primer widget
    }

    @Override
    public void onDisabled(Context context) {
        // Ejecutado cuando se elimina el último widget
    }
}
