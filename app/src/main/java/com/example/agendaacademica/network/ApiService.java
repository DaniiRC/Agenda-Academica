package com.example.agendaacademica.network;

import com.example.agendaacademica.model.Asignatura;
import com.example.agendaacademica.model.Evento;
import com.example.agendaacademica.model.Grupo;
import com.example.agendaacademica.model.LoginResponse;
import com.example.agendaacademica.model.Usuario;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ==========================================
    // 1. USUARIOS
    // ==========================================
    @POST("/api/usuarios/login")
    Call<LoginResponse> login(@Body Usuario usuario);

    @POST("/api/usuarios/registro")
    Call<LoginResponse> registrarUsuario(@Body Usuario usuario);

    @POST("/api/usuarios/enviar-codigo")
    Call<Void> enviarCodigo(@Query("email") String email);

    @POST("/api/usuarios/verificar-y-cambiar")
    Call<Void> verificarYCambiar(
            @Query("email") String email,
            @Query("codigo") String codigo,
            @Query("nuevaPassword") String nuevaPassword
    );

    @GET("/api/usuarios/{id}")
    Call<Usuario> obtenerUsuario(@Path("id") Long id);

    @POST("/api/usuarios/{id}/eliminar")
    Call<Void> eliminarUsuario(@Path("id") Long id);

    @Multipart
    @POST("/api/usuarios/{id}/foto")
    Call<Usuario> subirFotoUsuario(@Path("id") Long id, @Part MultipartBody.Part foto);

    @GET("/api/usuarios/todos")
    Call<List<Usuario>> obtenerTodosLosUsuarios();

    @PUT("/api/usuarios/{id}")
    Call<Usuario> actualizarUsuarioAdmin(@Path("id") Long id, @Body Usuario usuario);

    // ==========================================
    // 2. GRUPOS (Clases)
    // ==========================================
    @GET("/api/grupos/codigo/{codigo}")
    Call<Grupo> buscarGrupoPorCodigo(@Path("codigo") String codigo);

    @POST("/api/grupos/crear")
    Call<Grupo> crearGrupo(
            @Query("nombre") String nombre,
            @Query("descripcion") String descripcion,
            @Query("color") String color,
            @Query("profesorId") Long profesorId
    );

    @Multipart
    @POST("/api/grupos/{id}/foto")
    Call<Grupo> subirFotoGrupo(@Path("id") Long id, @Part MultipartBody.Part foto);

    // [ACTUALIZADO] Ahora el código va en la URL y el ID del usuario como parámetro
    @POST("/api/grupos/{codigo}/unirse")
    Call<Grupo> unirseAGrupo(@Path("codigo") String codigo, @Query("usuarioId") Long usuarioId);

    // [ACTUALIZADO] Ahora es un POST y el usuarioId va como parámetro
    @POST("/api/grupos/{grupoId}/salir")
    Call<Void> salirDeGrupo(@Path("grupoId") Long grupoId, @Query("usuarioId") Long usuarioId);

    @GET("/api/grupos/usuario/{usuarioId}")
    Call<List<Grupo>> obtenerGruposDeUsuario(@Path("usuarioId") Long usuarioId);

    @PUT("/api/grupos/{id}")
    Call<Grupo> actualizarGrupo(@Path("id") Long id, @Body Grupo grupo);

    @GET("/api/grupos/{codigo}/participantes")
    Call<List<Usuario>> obtenerParticipantes(@Path("codigo") String codigo);

    // ==========================================
    // 3. ASIGNATURAS
    // ==========================================
    @GET("/api/asignaturas/usuario/{usuarioId}")
    Call<List<Asignatura>> obtenerAsignaturasDeUsuario(@Path("usuarioId") Long usuarioId);

    // [NUEVO] Para cargar los "chips" de filtro en el perfil de la clase
    @GET("/api/asignaturas/grupo/{grupoId}")
    Call<List<Asignatura>> obtenerAsignaturasDeGrupo(@Path("grupoId") Long grupoId);

    // [CORREGIDO] Debe coincidir con el backend: @PostMapping("/grupo/{grupoId}")
    @POST("/api/asignaturas/grupo/{grupoId}")
    Call<Asignatura> crearAsignatura(@Path("grupoId") Long grupoId, @Body Asignatura asignatura);

    @DELETE("/api/asignaturas/{id}")
    Call<Void> eliminarAsignatura(@Path("id") Long id);

    // ==========================================
    // 4. EVENTOS Y TAREAS (Agenda)
    // ==========================================

    // Trae eventos personales + eventos de todos los grupos del usuario
    @GET("/api/eventos/usuario/{usuarioId}")
    Call<List<Evento>> obtenerEventosUsuario(@Path("usuarioId") Long usuarioId);

    // Trae eventos específicos de una clase
    @GET("/api/eventos/grupo/{grupoId}")
    Call<List<Evento>> obtenerEventosDeGrupo(@Path("grupoId") Long grupoId);

    @POST("/api/eventos/crear")
    Call<Evento> crearEvento(@Body Evento evento);

    @PUT("/api/eventos/{id}")
    Call<Evento> actualizarEvento(@Path("id") Long id, @Body Evento evento);

    @DELETE("/api/eventos/{id}")
    Call<Void> eliminarEvento(@Path("id") Long id);

    @GET("/api/eventos/{id}")
    Call<Evento> obtenerEventoPorId(@Path("id") Long id);

    @POST("/api/eventos/{id}/completar")
    Call<Void> marcarEventoCompletado(@Path("id") Long id, @Query("usuarioId") Long usuarioId);

    @POST("/api/eventos/{id}/tiempo-enfoque")
    Call<Void> guardarTiempoEnfoque(@Path("id") Long id, @Query("tiempoRestante") Long tiempoRestante);

    @POST("/api/eventos/{id}/tiempo-invertido")
    Call<Void> guardarTiempoInvertido(@Path("id") Long id, @Query("tiempoInvertido") Long tiempoInvertido);

    @POST("/api/eventos/{id}/nota")
    Call<Void> actualizarNota(@Path("id") Long id, @Query("nota") Double nota);

    @PUT("/api/subtareas/{id}")
    Call<Void> actualizarSubtarea(@Path("id") Long id, @Query("completada") boolean completada);

}