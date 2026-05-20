package com.example.edusync;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestor de persistencia local para la sesión del usuario y preferencias de la aplicación.
 * Utiliza {@link android.content.SharedPreferences} para almacenar información persistente como tokens de autenticación,
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
     * Persiste los datos básicos del usuario autenticado.
     *
     * @param id      Identificador único del usuario en el servidor.
     * @param nombre  Nombre completo del usuario.
     * @param email   Correo electrónico del usuario.
     * @param fotoUrl URL relativa o absoluta de la foto de perfil.
     * @param rol     Rol del usuario ({@code "USER"} o {@code "ADMIN"}).
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
     * Almacena el token JWT utilizado para autenticar las peticiones a la API.
     *
     * @param token Token JWT devuelto por el servidor tras el inicio de sesión.
     */
    public void guardarToken(String token) {
        editor.putString(KEY_JWT_TOKEN, token).apply();
    }

    /**
     * Recupera el token JWT de la sesión activa.
     *
     * @return El token JWT como {@link String}, o {@code null} si no hay sesión iniciada.
     */
    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    /**
     * Almacena el identificador y nombre del grupo académico activo del usuario.
     *
     * @param id     Identificador del grupo en el servidor.
     * @param nombre Nombre legible del grupo (p. ej. {@code "2º DAM B"}).
     */
    public void guardarGrupo(Long id, String nombre) {
        editor.putLong(KEY_GRUPO_ID, id);
        editor.putString(KEY_GRUPO_NOMBRE, nombre);
        editor.apply();
    }

    /**
     * @return ID del usuario en sesión, o {@code -1} si no hay sesión activa.
     */
    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    /**
     * @return ID del grupo académico activo, o {@code -1} si el usuario no pertenece a ninguno.
     */
    public Long obtenerGrupoId() {
        return prefs.getLong(KEY_GRUPO_ID, -1);
    }

    /**
     * @return Nombre del grupo académico activo, o {@code "Sin Grupo"} si no hay ninguno asignado.
     */
    public String obtenerNombreGrupo() {
        return prefs.getString(KEY_GRUPO_NOMBRE, "Sin Grupo");
    }

    /**
     * @return Nombre completo del usuario en sesión, o {@code "Usuario"} si no hay sesión.
     */
    public String obtenerNombreUsuario() {
        return prefs.getString(KEY_USER_NOMBRE, "Usuario");
    }

    /**
     * @return Correo electrónico del usuario en sesión, o {@code "Sin email"} si no hay sesión.
     */
    public String obtenerEmailUsuario() {
        return prefs.getString(KEY_USER_EMAIL, "Sin email");
    }

    /**
     * @return URL de la foto de perfil del usuario, o {@code null} si no tiene foto asignada.
     */
    public String obtenerFotoUsuario() {
        return prefs.getString(KEY_USER_FOTO, null);
    }

    /**
     * @return Rol del usuario ({@code "USER"} o {@code "ADMIN"}). Por defecto {@code "USER"}.
     */
    public String obtenerRol() {
        return prefs.getString(KEY_USER_ROL, "USER");
    }

    /**
     * @return {@code true} si el usuario en sesión tiene el rol {@code ADMIN}.
     */
    public boolean esAdmin() {
        return "ADMIN".equals(obtenerRol());
    }

    /**
     * Actualiza la URL de la foto de perfil sin modificar el resto de datos de sesión.
     *
     * @param fotoUrl Nueva URL de la foto de perfil.
     */
    public void actualizarFotoUsuario(String fotoUrl) {
        editor.putString(KEY_USER_FOTO, fotoUrl).apply();
    }

    /**
     * Indica si existe una sesión de usuario activa comprobando la presencia del ID de usuario.
     *
     * @return {@code true} si hay una sesión activa, {@code false} en caso contrario.
     */
    public boolean estaLogueado() {
        return getUserId() != -1;
    }

    /**
     * Elimina todos los datos de sesión del usuario y restaura las preferencias de la aplicación
     * (tema, idioma, accesibilidad) que deben persistir entre cuentas.
     */
    public void cerrarSesion() {
        boolean modoClaro = esModoClaro();
        String idioma    = obtenerIdioma();
        boolean haptic   = hapticFeedbackActivo();
        boolean anims    = animacionesActivas();

        editor.clear();
        guardarModoClaro(modoClaro);
        guardarIdioma(idioma);
        guardarHapticFeedback(haptic);
        guardarAnimacionesActivas(anims);
        editor.apply();
    }

    /**
     * Persiste la preferencia del tema visual.
     *
     * @param activado {@code true} para modo claro, {@code false} para modo oscuro.
     */
    public void guardarModoClaro(boolean activado) {
        editor.putBoolean(KEY_MODO_OSCURO, activado).apply();
    }

    /**
     * @return {@code true} si el modo claro está activo; {@code false} si el modo oscuro está activo.
     */
    public boolean esModoClaro() {
        return prefs.getBoolean(KEY_MODO_OSCURO, false);
    }

    /**
     * Habilita o deshabilita los recordatorios de notificaciones push.
     *
     * @param activos {@code true} para habilitar los recordatorios, {@code false} para desactivarlos.
     */
    public void guardarRecordatoriosActivos(boolean activos) {
        editor.putBoolean(KEY_RECORDATORIOS, activos).apply();
    }

    /**
     * @return {@code true} si los recordatorios de notificaciones están habilitados (valor por defecto).
     */
    public boolean recordatoriosActivos() {
        return prefs.getBoolean(KEY_RECORDATORIOS, true);
    }

    /**
     * Almacena el código de idioma seleccionado por el usuario.
     *
     * @param codigo Código ISO 639-1 del idioma (p. ej. {@code "es"}, {@code "en"}, {@code "fr"}).
     */
    public void guardarIdioma(String codigo) {
        editor.putString(KEY_IDIOMA, codigo).apply();
    }

    /**
     * @return Código ISO 639-1 del idioma guardado, o {@code null} si el usuario no ha elegido ninguno.
     */
    public String obtenerIdioma() {
        return prefs.getString(KEY_IDIOMA, null);
    }


    /**
     * @param activo {@code true} para habilitar la vibración táctil en interacciones.
     */
    public void guardarHapticFeedback(boolean activo) {
        editor.putBoolean(KEY_HAPTIC, activo).apply();
    }

    /**
     * @return {@code true} si el feedback háptico está habilitado (valor por defecto).
     */
    public boolean hapticFeedbackActivo() {
        return prefs.getBoolean(KEY_HAPTIC, true);
    }

    /**
     * @param activo {@code true} para habilitar las animaciones de transición entre pantallas.
     */
    public void guardarAnimacionesActivas(boolean activo) {
        editor.putBoolean(KEY_ANIMATIONS, activo).apply();
    }

    /**
     * @return {@code true} si las animaciones de interfaz están habilitadas (valor por defecto).
     */
    public boolean animacionesActivas() {
        return prefs.getBoolean(KEY_ANIMATIONS, true);
    }

    /**
     * @param activo {@code true} para activar el filtro de alto contraste en toda la aplicación.
     */
    public void guardarAltoContraste(boolean activo) {
        editor.putBoolean(KEY_HIGH_CONTRAST, activo).apply();
    }

    /**
     * @return {@code true} si el modo de alto contraste está activo.
     */
    public boolean altoContrasteActivo() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    /**
     * @param activo {@code true} para activar la corrección de color para daltonismo (deuteranopia).
     */
    public void guardarModoDaltonico(boolean activo) {
        editor.putBoolean(KEY_COLOR_BLIND, activo).apply();
    }

    /**
     * @return {@code true} si el modo de corrección para daltonismo está activo.
     */
    public boolean modoDaltonicoActivo() {
        return prefs.getBoolean(KEY_COLOR_BLIND, false);
    }

    /**
     * Aplica el modo noche o claro de forma global antes de que se infle ninguna actividad.
     * Debe invocarse desde {@link BaseActivity#onCreate} antes de {@code super.onCreate()}.
     *
     * @param context Contexto de la aplicación necesario para acceder a las preferencias.
     */
    public static void aplicarTemaGlobal(Context context) {
        SessionManager sm = new SessionManager(context);
        boolean esClaro = sm.esModoClaro();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                esClaro ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO 
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        );
    }
}
