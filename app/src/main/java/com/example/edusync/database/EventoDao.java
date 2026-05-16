package com.example.edusync.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventoDao {
    @Query("SELECT * FROM eventos ORDER BY fecha ASC")
    LiveData<List<EventoEntity>> obtenerTodosLosEventos();

    @Query("SELECT * FROM eventos WHERE id = :id")
    LiveData<EventoEntity> obtenerEventoPorId(Long id);

    @Query("SELECT * FROM eventos WHERE fecha LIKE :fecha || '%' ORDER BY hora ASC")
    List<EventoEntity> obtenerTareasDeHoySincrono(String fecha);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarEventos(List<EventoEntity> eventos);

    @Query("DELETE FROM eventos")
    void borrarTodosLosEventos();
}
