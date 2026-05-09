package com.example.agendaacademica.database;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EventoDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private EventoDao eventoDao;

    @Before
    public void createDb() {
        // Arrange: Iniciar DB en memoria para pruebas
        Context context = ApplicationProvider.getApplicationContext();
        
        // Usamos inMemoryDatabaseBuilder para que los datos desaparezcan al terminar
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries() // Permitido SOLO en tests para evitar asincronía compleja
                .build();
        eventoDao = db.eventoDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertarYLeerEventos_DeberiaGuardarCorrectamente() {
        // Arrange
        EventoEntity evento1 = new EventoEntity(1L, "Test Examen", "Estudiar Mockito", "2026-06-15", "09:00", "Examen", "QA Group", false);
        EventoEntity evento2 = new EventoEntity(2L, "Test Práctica", "Laboratorio", "2026-06-16", "11:00", "Tarea", "General", false);
        List<EventoEntity> eventosAInsertar = Arrays.asList(evento1, evento2);

        // Act
        eventoDao.insertarEventos(eventosAInsertar);
        
        // Utilizamos el método síncrono que ya tienes declarado para validar rápidamente
        List<EventoEntity> eventosHoy = eventoDao.obtenerTareasDeHoySincrono("2026-06-15");

        // Assert
        assertNotNull(eventosHoy);
        assertEquals(1, eventosHoy.size());
        assertEquals("Test Examen", eventosHoy.get(0).getTitulo());
        assertEquals("QA Group", eventosHoy.get(0).getNombreGrupo());
    }

    @Test
    public void borrarTodosLosEventos_DeberiaVaciarLaTabla() {
        // Arrange
        EventoEntity evento1 = new EventoEntity(1L, "Examen de prueba", "Desc", "2026-06-15", "09:00", "Examen", "General", false);
        eventoDao.insertarEventos(Arrays.asList(evento1));

        // Act
        eventoDao.borrarTodosLosEventos();
        List<EventoEntity> despuesDeBorrar = eventoDao.obtenerTareasDeHoySincrono("2026-06-15");

        // Assert
        assertTrue(despuesDeBorrar.isEmpty());
    }
}
