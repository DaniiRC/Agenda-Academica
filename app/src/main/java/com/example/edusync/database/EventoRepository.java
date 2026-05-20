package com.example.edusync.database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.edusync.model.Evento;
import com.example.edusync.network.RetrofitClient;

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
     * Guarda la lista de eventos recibida en la base de datos local (Room).
     * Primero borra todos los registros existentes para evitar duplicaciones.
     */
    public void guardarEventosEnLocal(List<Evento> eventos) {
        if (eventos == null) return;
        List<EventoEntity> entities = new ArrayList<>();
        for (Evento e : eventos) {
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
            eventoDao.resetearEventos(entities);
            Log.d("REPOSITORY", "Datos sincronizados en Room");
        });
    }

    /**
     * Sincroniza los datos de la API con la base de datos local.
     */
    public void refreshEventos(Long usuarioId) {
        RetrofitClient.getApiService().obtenerEventosUsuario(usuarioId).enqueue(new Callback<List<Evento>>() {
            @Override
            public void onResponse(Call<List<Evento>> call, Response<List<Evento>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarEventosEnLocal(response.body());
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
