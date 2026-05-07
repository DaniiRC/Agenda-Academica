package com.example.agendaacademica.database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.agendaacademica.model.Evento;
import com.example.agendaacademica.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventoRepository {
    private final Application application;
    private final EventoDao eventoDao;
    private final LiveData<List<EventoEntity>> todosLosEventos;
    private final ExecutorService executorService;

    public EventoRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        eventoDao = db.eventoDao();
        todosLosEventos = eventoDao.obtenerTodosLosEventos();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Expone el LiveData de la DB local hacia la UI.
     * Este es el "Single Source of Truth".
     */
    public LiveData<List<EventoEntity>> getTodosLosEventos() {
        return todosLosEventos;
    }

    public LiveData<EventoEntity> getEventoPorId(Long id) {
        return eventoDao.obtenerEventoPorId(id);
    }

    /**
     * Sincroniza los datos de la API con la base de datos local.
     */
    public void refreshEventos(Long usuarioId) {
        RetrofitClient.getApiService().obtenerEventosUsuario(usuarioId).enqueue(new Callback<List<Evento>>() {
            @Override
            public void onResponse(Call<List<Evento>> call, Response<List<Evento>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Mapear de Evento (API) a EventoEntity (Room)
                    List<EventoEntity> entities = new ArrayList<>();
                    for (Evento e : response.body()) {
                        String rawFecha = e.getFecha();
                        String fechaLimpia = "2000-01-01";
                        if (rawFecha != null) {
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(rawFecha);
                            if (m.find()) fechaLimpia = m.group(1);
                            else fechaLimpia = rawFecha;
                        }

                        entities.add(new EventoEntity(
                                e.getId(),
                                e.getTitulo(),
                                e.getDescripcion(),
                                fechaLimpia,
                                e.getHora(),
                                e.getTipo(),
                                e.getGrupo() != null ? e.getGrupo().getNombre() : "General",
                                e.isCompletado()
                        ));
                    }

                    // Operación en segundo plano usando ExecutorService
                    executorService.execute(() -> {
                        eventoDao.borrarTodosLosEventos();
                        eventoDao.insertarEventos(entities);
                        Log.d("REPOSITORY", "Datos sincronizados en Room");

                        // Notificar al Widget que los datos han cambiado
                        android.content.Intent intent = new android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        intent.setComponent(new android.content.ComponentName(application, com.example.agendaacademica.widget.AgendaWidget.class));
                        application.sendBroadcast(intent);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Evento>> call, Throwable t) {
                Log.e("REPOSITORY", "Fallo al refrescar desde API: " + t.getMessage());
                // No hacemos nada, la UI seguirá viendo los datos antiguos de Room
            }
        });
    }
}
