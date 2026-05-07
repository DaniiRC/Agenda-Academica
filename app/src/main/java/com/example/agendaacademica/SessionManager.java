package com.example.agendaacademica;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestor de persistencia local para la sesión del usuario y preferencias de la aplicación.
 * Utiliza {@link SharedPreferences} para almacenar información persistente como tokens de autenticación,
 * datos del perfil del usuario, configuración del tema, idioma y estado de notificaciones.
 */
public class SessionManager {
    private static final String PREF_NAME = "AgendaPrefs";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_USER_NOMBRE = "USER_NOMBRE";
    private static final String KEY_USER_EMAIL = "USER_EMAIL";
    private static final String KEY_USER_FOTO = "USER_FOTO";
    private static final String KEY_USER_ROL = "USER_ROL";
    private static final String KEY_JWT_TOKEN = "JWT_TOKEN";
    private static final String KEY_GRUPO_ID = "GRUPO_ID";
    private static final String KEY_GRUPO_NOMBRE = "GRUPO_NOMBRE";
    private static final String KEY_MODO_OSCURO = "MODO_OSCURO";
    private static final String KEY_RECORDATORIOS = "RECORDATORIOS_ACTIVOS";
    private static final String KEY_IDIOMA = "IDIOMA_APP";
    private static final String KEY_EVENTOS_CACHE = "EVENTOS_CACHE";
    private static final String KEY_HAPTIC = "HAPTIC_FEEDBACK";
    private static final String KEY_ANIMATIONS = "ANIMATIONS_ENABLED";
    private static final String KEY_HIGH_CONTRAST = "HIGH_CONTRAST";
    private static final String KEY_COLOR_BLIND = "COLOR_BLIND_MODE";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    /**
     * Constructor que inicializa las preferencias compartidas en modo privado.
     * 
     * @param context Contexto de la aplicación.
     */
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Persiste la información básica del usuario.
     * 
     * @param id      ID único del usuario.
     * @param nombre  Nombre completo.
     * @param email   Correo electrónico.
     * @param fotoUrl URL de la foto de perfil.
     */
    public void guardarUsuario(Long id, String nombre, String email, String fotoUrl, String rol) {
        editor.putLong(KEY_USER_ID, id);
        editor.putString(KEY_USER_NOMBRE, nombre);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_FOTO, fotoUrl);
        editor.putString(KEY_USER_ROL, rol);
        editor.apply();
    }

    /**
     * Almacena el token JWT para autenticar las peticiones a la API.
     * 
     * @param token Token de seguridad.
     */
    public void guardarToken(String token) {
        editor.putString(KEY_JWT_TOKEN, token).apply();
    }

    /**
     * Recupera el token JWT almacenado.
     * 
     * @return El token o null si no existe.
     */
    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    /**
     * Almacena los datos del grupo académico al que pertenece el usuario.
     * 
     * @param id     ID del grupo.
     * @param nombre Nombre del grupo.
     */
    public void guardarGrupo(Long id, String nombre) {
        editor.putLong(KEY_GRUPO_ID, id);
        editor.putString(KEY_GRUPO_NOMBRE, nombre);
        editor.apply();
    }

    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public Long obtenerGrupoId() {
        return prefs.getLong(KEY_GRUPO_ID, -1);
    }

    public String obtenerNombreGrupo() {
        return prefs.getString(KEY_GRUPO_NOMBRE, "Sin Grupo");
    }

    public String obtenerNombreUsuario() {
        return prefs.getString(KEY_USER_NOMBRE, "Usuario");
    }

    public String obtenerEmailUsuario() {
        return prefs.getString(KEY_USER_EMAIL, "Sin email");
    }

    public String obtenerFotoUsuario() {
        return prefs.getString(KEY_USER_FOTO, null);
    }

    public String obtenerRol() {
        return prefs.getString(KEY_USER_ROL, "USER");
    }

    public boolean esAdmin() {
        return "ADMIN".equals(obtenerRol());
    }

    public void actualizarFotoUsuario(String fotoUrl) {
        editor.putString(KEY_USER_FOTO, fotoUrl).apply();
    }

    /**
     * Verifica si existe una sesión de usuario activa.
     * 
     * @return Verdadero si el usuario está logueado.
     */
    public boolean estaLogueado() {
        return getUserId() != -1;
    }

    /**
     * Elimina todos los datos de sesión almacenados.
     */
    public void cerrarSesion() {
        editor.clear().apply();
    }

    /**
     * Guarda la preferencia del tema visual.
     * 
     * @param activado Verdadero para modo claro, falso para modo oscuro.
     */
    public void guardarModoClaro(boolean activado) {
        editor.putBoolean(KEY_MODO_OSCURO, activado).apply();
    }

    public boolean esModoClaro() {
        return prefs.getBoolean(KEY_MODO_OSCURO, false);
    }

    /**
     * Habilita o deshabilita los recordatorios de notificaciones.
     * 
     * @param activos Estado deseado de las notificaciones.
     */
    public void guardarRecordatoriosActivos(boolean activos) {
        editor.putBoolean(KEY_RECORDATORIOS, activos).apply();
    }

    public boolean recordatoriosActivos() {
        return prefs.getBoolean(KEY_RECORDATORIOS, true);
    }

    /**
     * Almacena el código de idioma seleccionado.
     * 
     * @param codigo Código ISO del idioma (ej: "es", "en").
     */
    public void guardarIdioma(String codigo) {
        editor.putString(KEY_IDIOMA, codigo).apply();
    }

    public String obtenerIdioma() {
        return prefs.getString(KEY_IDIOMA, "es");
    }

    /**
     * Cachea los eventos en formato JSON para permitir el acceso offline.
     * 
     * @param json Cadena JSON con la lista de eventos.
     */
    public void guardarEventosCache(String json) {
        editor.putString(KEY_EVENTOS_CACHE, json).apply();
    }

    public String obtenerEventosCache() {
        return prefs.getString(KEY_EVENTOS_CACHE, null);
    }

    public void guardarHapticFeedback(boolean activo) {
        editor.putBoolean(KEY_HAPTIC, activo).apply();
    }

    public boolean hapticFeedbackActivo() {
        return prefs.getBoolean(KEY_HAPTIC, true);
    }

    public void guardarAnimacionesActivas(boolean activo) {
        editor.putBoolean(KEY_ANIMATIONS, activo).apply();
    }

    public boolean animacionesActivas() {
        return prefs.getBoolean(KEY_ANIMATIONS, true);
    }

    public void guardarAltoContraste(boolean activo) {
        editor.putBoolean(KEY_HIGH_CONTRAST, activo).apply();
    }

    public boolean altoContrasteActivo() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    public void guardarModoDaltonico(boolean activo) {
        editor.putBoolean(KEY_COLOR_BLIND, activo).apply();
    }

    public boolean modoDaltonicoActivo() {
        return prefs.getBoolean(KEY_COLOR_BLIND, false);
    }
}
