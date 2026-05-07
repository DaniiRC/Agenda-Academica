package com.example.agendaacademica.network;

import android.content.Context;
import com.example.agendaacademica.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // [CONFIGURACIÓN] IP local para pruebas
    // private static final String IP_SERVIDOR = "192.168.100.189";
    // public static final String BASE_URL = "http://" + IP_SERVIDOR + ":8080/";
    
    // [CONFIGURACIÓN] URL de Producción (Render)
    public static final String BASE_URL = "https://proyecto-final-api-1.onrender.com/";

    private static Retrofit retrofit = null;

    public static void init(Context context) {
        if (retrofit == null) {

            // Referencia al SessionManager para leer el token en cada petición
            SessionManager sessionManager = new SessionManager(context.getApplicationContext());

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    // Interceptor de errores ya existente
                    .addInterceptor(new ErrorInterceptor(context))
                    // ── Interceptor JWT ──────────────────────────────────────
                    // Lee el token guardado en SharedPreferences y lo añade
                    // como "Authorization: Bearer <token>" a TODAS las peticiones.
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

    public static ApiService getApiService() {
        if (retrofit == null) {
            throw new IllegalStateException("RetrofitClient must be initialized with init(context) before use.");
        }
        return retrofit.create(ApiService.class);
    }
}

