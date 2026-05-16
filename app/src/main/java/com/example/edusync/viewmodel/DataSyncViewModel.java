package com.example.edusync.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DataSyncViewModel extends ViewModel {

    // Evento para avisar que la lista de la agenda debe refrescarse
    private final MutableLiveData<Boolean> _refreshAgenda = new MutableLiveData<>();
    public LiveData<Boolean> getRefreshAgenda() {
        return _refreshAgenda;
    }

    public void solicitarRefrescoAgenda() {
        _refreshAgenda.setValue(true);
    }
}
