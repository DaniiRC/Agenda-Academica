package com.example.edusync;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.edusync.network.RetrofitClient;

/**
 * Clase de aplicación personalizada.
 * Se ejecuta antes que cualquier actividad o servicio y es responsable de:
 * - Inicializar el cliente Retrofit con el contexto de la aplicación.
 * - Restaurar el idioma guardado por el usuario mediante AppCompatDelegate.
 */
public class EduSyncApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);

        SessionManager session = new SessionManager(this);
        String language = session.obtenerIdioma();

        if (language == null) {
            return;
        }

        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        LocaleListCompat desired = LocaleListCompat.forLanguageTags(language);

        if (current.isEmpty() || !current.toLanguageTags().startsWith(language)) {
            AppCompatDelegate.setApplicationLocales(desired);
        }
    }
}
