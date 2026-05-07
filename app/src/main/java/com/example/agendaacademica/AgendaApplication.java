package com.example.agendaacademica;

import android.app.Application;
import com.example.agendaacademica.network.RetrofitClient;

public class AgendaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);

        // Asegurar que el idioma configurado (o Español por defecto) se aplique al iniciar
        SessionManager session = new SessionManager(this);
        String language = session.obtenerIdioma();
        androidx.core.os.LocaleListCompat appLocales = androidx.core.os.LocaleListCompat.forLanguageTags(language);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocales);
    }
}
