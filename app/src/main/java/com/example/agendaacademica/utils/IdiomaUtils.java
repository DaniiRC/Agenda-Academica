package com.example.agendaacademica.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Clase de utilidad para la configuración manual del idioma de la aplicación.
 * Se usa como alternativa de compatibilidad para dispositivos que no soportan
 * {@link androidx.appcompat.app.AppCompatDelegate#setApplicationLocales}.
 *
 * @deprecated Para nuevos desarrollos, prefer AppCompatDelegate.setApplicationLocales().
 */
public class IdiomaUtils {

    /**
     * Aplica un idioma específico al contexto de la aplicación.
     *
     * @param context        Contexto original de la aplicación.
     * @param codigoIdioma   Código ISO 639-1 del idioma (ej: "es", "en", "fr").
     * @return Contexto actualizado con la configuración regional aplicada.
     */
    public static Context configurarIdioma(Context context, String codigoIdioma) {
        Locale locale = new Locale(codigoIdioma);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            return context.createConfigurationContext(config);
        } else {
            config.setLocale(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
