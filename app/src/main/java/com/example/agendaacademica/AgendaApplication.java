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
