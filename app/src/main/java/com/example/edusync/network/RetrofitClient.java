package com.example.edusync.network;

import android.content.Context;
import com.example.edusync.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton que inicializa y proporciona acceso al cliente Retrofit para las llamadas a la API REST.
 * Configura un interceptor JWT que adjunta automáticamente el token de sesión a todas las peticiones.
 */
public class RetrofitClient {

    // URL de producción desplegada en Render. Comentar y usar IP local para pruebas en red local.
    // private static final String IP_SERVIDOR = "192.168.100.189";
    // public static final String BASE_URL = "http://" + IP_SERVIDOR + ":8080/";
    public static final String BASE_URL = "https://proyecto-final-api-1.onrender.com/";
    //public static final String BASE_URL = "http://192.168.100.189:8080/";

    private static Retrofit retrofit = null;

    /**
     * Inicializa el cliente Retrofit. Debe llamarse una única vez desde {@link com.example.edusync.EduSyncApplication}.
     *
     * @param context Contexto de la aplicación.
     */
    public static void init(Context context) {
        if (retrofit == null) {

            // Referencia al SessionManager para leer el token en cada petición
            SessionManager sessionManager = new SessionManager(context.getApplicationContext());

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    // Interceptor JWT: añade el token de sesión a todas las peticiones salientes.
                    .addInterceptor(new ErrorInterceptor(context))
                    .addInterceptor(chain -> {
                        Request originalRequest = chain.request();
                        String token = sessionManager.getToken();

                        if (token != null && !token.isEmpty()) {
                            Request authenticatedRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(authenticatedRequest);
                        }

                        return chain.proceed(originalRequest);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
    }

    /**
     * Devuelve la instancia única del servicio de API.
     *
     * @throws IllegalStateException Si {@link #init(Context)} no fue llamado previamente.
     */
    public static ApiService getApiService() {
        if (retrofit == null) {
            throw new IllegalStateException("RetrofitClient must be initialized with init(context) before use.");
        }
        return retrofit.create(ApiService.class);
    }
}

