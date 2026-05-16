package com.example.edusync.repository;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.edusync.database.AppDatabase;
import com.example.edusync.database.EventoDao;
import com.example.edusync.database.EventoEntity;
import com.example.edusync.database.EventoRepository;
import com.example.edusync.model.Evento;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventoRepositoryTest {

    // Regla obligatoria para que LiveData se ejecute de forma síncrona en el hilo de pruebas
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Application mockApplication;

    @Mock
    private AppDatabase mockDatabase;

    @Mock
    private EventoDao mockEventoDao;

    @Mock
    private ApiService mockApiService;

    @Mock
    private Call<List<Evento>> mockCall;

    private MockedStatic<AppDatabase> mockedAppDatabase;
    private MockedStatic<RetrofitClient> mockedRetrofitClient;
    private MockedStatic<android.util.Log> mockedLog;
    private EventoRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Arrange: Mockeamos el Room Database para aislarlo del contexto de Android
        mockedAppDatabase = Mockito.mockStatic(AppDatabase.class);
        mockedAppDatabase.when(() -> AppDatabase.getDatabase(mockApplication)).thenReturn(mockDatabase);
        when(mockDatabase.eventoDao()).thenReturn(mockEventoDao);

        // Simulamos un valor inicial en LiveData para que el contructor del Repository no falle
        MutableLiveData<List<EventoEntity>> liveData = new MutableLiveData<>(new ArrayList<>());
        when(mockEventoDao.obtenerTodosLosEventos()).thenReturn(liveData);

        // Mockeamos el patrón Singleton de RetrofitClient
        mockedRetrofitClient = Mockito.mockStatic(RetrofitClient.class);
        mockedRetrofitClient.when(RetrofitClient::getApiService).thenReturn(mockApiService);

        mockedLog = Mockito.mockStatic(android.util.Log.class);

        // Inicializamos el Repositorio inyectando la App mockeada
        repository = new EventoRepository(mockApplication);
    }

    @After
    public void tearDown() {
        // Liberamos los mocks estáticos
        mockedAppDatabase.close();
        mockedRetrofitClient.close();
        mockedLog.close();
    }

    @Test
    public void refreshEventos_FalloApi_MantieneDatosDeRoom() {
        // Arrange
        Long usuarioId = 1L;
        when(mockApiService.obtenerEventosUsuario(usuarioId)).thenReturn(mockCall);

        // Simulamos un error de conexión (el móvil no tiene internet o servidor caído)
        doAnswer(invocation -> {
            Callback<List<Evento>> callback = invocation.getArgument(0);
            callback.onFailure(mockCall, new Throwable("Sin internet"));
            return null;
        }).when(mockCall).enqueue(any());

        // Act
        repository.refreshEventos(usuarioId);

        // Assert
        // ¡CRÍTICO PARA LA ÚNICA FUENTE DE LA VERDAD!
        // Aseguramos que NUNCA se borren los eventos de Room si la API falla.
        verify(mockEventoDao, never()).borrarTodosLosEventos();
        verify(mockEventoDao, never()).insertarEventos(anyList());
    }

    @Test
    public void refreshEventos_ExitoApi_SincronizaYGuardaEnLocal() throws InterruptedException {
        // Arrange
        Long usuarioId = 1L;
        when(mockApiService.obtenerEventosUsuario(usuarioId)).thenReturn(mockCall);

        List<Evento> mockEventosApi = new ArrayList<>();
        Evento eventoApi = new Evento();
        eventoApi.setId(10L);
        eventoApi.setTitulo("Nueva Tarea Sincronizada");
        mockEventosApi.add(eventoApi);
        
        Response<List<Evento>> responseSuccess = Response.success(mockEventosApi);

        // Simulamos éxito en la llamada a la API
        doAnswer(invocation -> {
            Callback<List<Evento>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, responseSuccess);
            return null;
        }).when(mockCall).enqueue(any());

        // Act
        repository.refreshEventos(usuarioId);

        // Dormimos el hilo temporalmente para darle tiempo al ExecutorService interno del Repository
        Thread.sleep(200);

        // Assert
        // Verificamos que se borró el caché viejo y se guardó el nuevo.
        verify(mockEventoDao, Mockito.times(1)).borrarTodosLosEventos();
        verify(mockEventoDao, Mockito.times(1)).insertarEventos(anyList());
    }
}
