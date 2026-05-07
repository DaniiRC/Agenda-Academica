package com.example.agendaacademica.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.agendaacademica.database.EventoEntity;
import com.example.agendaacademica.database.EventoRepository;

import java.util.List;

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

    public void refresh(Long usuarioId) {
        repository.refreshEventos(usuarioId);
    }
}
