package com.example.agendaacademica;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.agendaacademica.network.RetrofitClient;

public class AgendaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);

        // Forzar siempre el idioma guardado (por defecto "es").
        // Necesario porque en primera instalación AppCompatDelegate hereda
        // el locale del sistema aunque el valor guardado sea "es".
        SessionManager session = new SessionManager(this);
        String language = session.obtenerIdioma(); // devuelve "es" si no hay nada guardado

        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        LocaleListCompat desired = LocaleListCompat.forLanguageTags(language);

        // Solo llamamos a setApplicationLocales si el locale activo no coincide
        // para evitar un reinicio de actividad innecesario
        if (current.isEmpty() || !current.toLanguageTags().startsWith(language)) {
            AppCompatDelegate.setApplicationLocales(desired);
        }
    }
}
