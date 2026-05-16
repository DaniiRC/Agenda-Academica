package com.example.edusync.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DataSyncViewModel extends ViewModel {

    // Evento para avisar que la lista de la agenda debe refrescarse
    private final MutableLiveData<Boolean> _refreshEduSync = new MutableLiveData<>();
    public LiveData<Boolean> getRefreshEduSync() {
        return _refreshEduSync;
    }

    public void solicitarRefrescoEduSync() {
        _refreshEduSync.setValue(true);
    }
}
