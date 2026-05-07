package com.example.agendaacademica.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.agendaacademica.SessionManager;

/**
 * BaseActivity simplificada. 
 * AppCompatDelegate se encarga automáticamente de aplicar los locales 
 * guardados por el sistema sin necesidad de envolver el contexto manualmente.
 */
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

public class BaseActivity extends AppCompatActivity {
    protected SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        aplicarAccesibilidad();
    }

    protected void aplicarAccesibilidad() {
        // 1. Animaciones
        if (!session.animacionesActivas()) {
            getWindow().setWindowAnimations(0);
        }

        // 2. Filtros de Color
        View decorView = getWindow().getDecorView();
        
        // Usamos post para asegurar que la vista esté lista y evitar el "parpadeo" o delay
        decorView.post(() -> {
            if (session.altoContrasteActivo() || session.modoDaltonicoActivo()) {
                ColorMatrix combinedMatrix = new ColorMatrix();
                
                if (session.modoDaltonicoActivo()) {
                    // Matriz de simulación/corrección profesional para Deuteranopía
                    // Esta matriz ayuda a que los verdes y rojos se diferencien mejor entre sí
                    ColorMatrix dm = new ColorMatrix(new float[] {
                        0.625f, 0.375f, 0,      0, 0,
                        0.7f,   0.3f,   0,      0, 0,
                        0,      0.3f,   0.7f,   0, 0,
                        0,      0,      0,      1, 0
                    });
                    combinedMatrix.postConcat(dm);
                }

                if (session.altoContrasteActivo()) {
                    ColorMatrix hc = new ColorMatrix();
                    // Aumentamos el contraste de forma equilibrada
                    float contrast = 1.3f;
                    float brightness = -15f; 
                    hc.set(new float[] {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                    });
                    combinedMatrix.postConcat(hc);
                    
                    // Reforzamos la saturación para que el color no se lave con el contraste
                    ColorMatrix sat = new ColorMatrix();
                    sat.setSaturation(1.4f);
                    combinedMatrix.postConcat(sat);
                }

                Paint paint = new Paint();
                paint.setColorFilter(new ColorMatrixColorFilter(combinedMatrix));
                decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            } else {
                // Si todo está desactivado, volvemos a la normalidad absoluta
                decorView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }
}
