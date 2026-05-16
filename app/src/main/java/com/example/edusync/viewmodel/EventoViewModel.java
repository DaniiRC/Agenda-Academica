package com.example.edusync.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.edusync.database.EventoEntity;
import com.example.edusync.database.EventoRepository;

import java.util.List;

/**
 * ViewModel que actúa como intermediario entre la capa de datos (Room) y la UI.
 * Expone los eventos almacenados localmente como flujos {@link LiveData} reactivos,
 * permitiendo que las vistas se actualicen automáticamente ante cambios en la base de datos.
 */
public class EventoViewModel extends AndroidViewModel {
    private final EventoRepository repository;
    private final LiveData<List<EventoEntity>> todosLosEventos;

    public EventoViewModel(@NonNull Application application) {
        super(application);
        repository = new EventoRepository(application);
        todosLosEventos = repository.getTodosLosEventos();
    }

    public LiveData<List<EventoEntity>> getTodosLosEventos() {
        return todosLosEventos;
    }

    public LiveData<EventoEntity> getEventoPorId(Long id) {
        return repository.getEventoPorId(id);
    }

    /**
     * Fuerza una sincronización de los eventos del usuario con el servidor
     * y actualiza la base de datos local.
     *
     * @param usuarioId ID del usuario cuya agenda se va a refrescar.
     */
    public void refresh(Long usuarioId) {
        repository.refreshEventos(usuarioId);
    }
}
