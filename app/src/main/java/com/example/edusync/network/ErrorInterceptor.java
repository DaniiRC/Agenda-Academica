package com.example.edusync.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ErrorInterceptor implements Interceptor {

    private final Context context;
    private static long lastErrorTime = 0;
    private static final long DEBOUNCE_TIME = 3000; // 3 segundos para evitar spam

    public ErrorInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        
        // Simplemente procedemos. Si hay error de red, Retrofit lanzará IOException
        // y lo capturaremos en el onFailure de cada Activity/Fragment para mostrar el banner.
        return chain.proceed(request);
    }
}
