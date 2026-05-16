package com.example.edusync.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.edusync.R;
import com.example.edusync.database.AppDatabase;
import com.example.edusync.database.EventoEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EduSyncWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AgendaDataProvider(this.getApplicationContext(), intent);
    }
}

class AgendaDataProvider implements RemoteViewsService.RemoteViewsFactory {
    private List<EventoEntity> listaTareas = new ArrayList<>();
    private Context context;

    public AgendaDataProvider(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        // Se deja vacío. onDataSetChanged() se encargará de la carga inicial en un hilo secundario.
    }

    @Override
    public void onDataSetChanged() {
        cargarDatos();
    }

    private void cargarDatos() {
        // Obtenemos los datos de Room directamente (Sincrónico ya que estamos en el hilo del Widget)
        String hoy = LocalDate.now().toString(); // Formato yyyy-MM-dd
        // Nota: Como es un Widget, necesitamos una consulta que no sea LiveData para obtener los datos actuales
        listaTareas = AppDatabase.getDatabase(context).eventoDao().obtenerTareasDeHoySincrono(hoy);
    }

    @Override
    public void onDestroy() {
        listaTareas.clear();
    }

    @Override
    public int getCount() {
        return listaTareas.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= listaTareas.size()) return null;

        EventoEntity tarea = listaTareas.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_tarea);
        
        views.setTextViewText(R.id.widgetItemTitle, tarea.getTitulo());
        
        String infoExtra = (tarea.getNombreGrupo() != null ? tarea.getNombreGrupo() : "General") 
                           + " • " + (tarea.getHora() != null ? tarea.getHora() : "--:--");
        views.setTextViewText(R.id.widgetItemTime, infoExtra);
        
        // Color basado en el tipo de tarea (igual que en la app)
        String colorHex = "#818CF8";
        if (tarea.getTipo() != null) {
            switch (tarea.getTipo()) {
                case "Deberes": colorHex = "#3B82F6"; break;
                case "Proyecto": colorHex = "#F59E0B"; break;
                case "Examen": colorHex = "#EF4444"; break;
                case "Estudio": colorHex = "#10B981"; break;
            }
        }
        views.setInt(R.id.widgetItemColor, "setColorFilter", android.graphics.Color.parseColor(colorHex));
        
        // Fill-in Intent para pasar el ID de la tarea
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("EVENTO_ID", tarea.getId());
        views.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
