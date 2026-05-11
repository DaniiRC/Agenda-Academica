package com.example.agendaacademica.activities;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agendaacademica.SessionManager;

/**
 * Actividad base de la que heredan todas las actividades de la aplicación.
 *
 * Aplica el tema visual (claro/oscuro) mediante {@link SessionManager#aplicarTemaGlobal(android.content.Context)}
 * antes del inflado de vistas. En cada reanudación, invoca {@link #aplicarAccesibilidad()} para
 * reflejar en tiempo real los filtros de alto contraste y daltonismo configurados por el usuario.
 *
 * La gestión del idioma (locale) se delega a {@code AppCompatDelegate}, que aplica
 * automáticamente la configuración regional almacenada mediante
 * {@link androidx.appcompat.app.AppCompatDelegate#setApplicationLocales}.
 */
public class BaseActivity extends AppCompatActivity {

    /** Gestor de sesión disponible para todas las subclases. */
    protected SessionManager session;

    /**
     * Aplica el tema antes de inflar vistas e inicializa el gestor de sesión.
     *
     * @param savedInstanceState Estado previo de la actividad, o {@code null} si es la primera vez.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SessionManager.aplicarTemaGlobal(this);
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
    }

    /**
     * Aplica los filtros de accesibilidad cada vez que la actividad vuelve a primer plano,
     * garantizando que los cambios de preferencias se reflejen sin necesidad de reiniciar.
     */
    @Override
    protected void onResume() {
        super.onResume();
        aplicarAccesibilidad();
    }

    /**
     * Aplica los filtros visuales de accesibilidad sobre la vista raíz de la ventana.
     *
     * Los filtros se componen mediante {@link ColorMatrix#postConcat} en el siguiente orden:
     * 1. Daltonismo: matriz de corrección para deuteranopia (déficit rojo-verde) que
     *    redistribuye los canales RGB para maximizar la distinción entre ambos colores.
     * 2. Alto contraste: factor de escala de 1.3 con reducción de brillo base (−15)
     *    para evitar sobrexposición, seguido de un aumento de saturación (1.4) que compensa
     *    el efecto de lavado de color.
     *
     * La vista se configura con {@link View#LAYER_TYPE_HARDWARE} para que el filtro se aplique
     * de forma eficiente por la GPU. Si ningún filtro está activo, se restablece
     * {@link View#LAYER_TYPE_NONE} para evitar consumo innecesario de memoria de vídeo.
     *
     * Se usa {@link View#post(Runnable)} para garantizar que la vista esté completamente
     * inflada antes de aplicar el filtro.
     */
    protected void aplicarAccesibilidad() {
        if (!session.animacionesActivas()) {
            getWindow().setWindowAnimations(0);
        }

        View decorView = getWindow().getDecorView();
        decorView.post(() -> {
            if (session.altoContrasteActivo() || session.modoDaltonicoActivo()) {
                ColorMatrix combinedMatrix = new ColorMatrix();

                if (session.modoDaltonicoActivo()) {
                    ColorMatrix dm = new ColorMatrix(new float[]{
                        0.625f, 0.375f, 0,      0, 0,
                        0.7f,   0.3f,   0,      0, 0,
                        0,      0.3f,   0.7f,   0, 0,
                        0,      0,      0,      1, 0
                    });
                    combinedMatrix.postConcat(dm);
                }

                if (session.altoContrasteActivo()) {
                    float contrast   = 1.3f;
                    float brightness = -15f;
                    ColorMatrix hc = new ColorMatrix(new float[]{
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0,        1, 0
                    });
                    combinedMatrix.postConcat(hc);

                    ColorMatrix sat = new ColorMatrix();
                    sat.setSaturation(1.4f);
                    combinedMatrix.postConcat(sat);
                }

                Paint paint = new Paint();
                paint.setColorFilter(new ColorMatrixColorFilter(combinedMatrix));
                decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            } else {
                decorView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }
}
