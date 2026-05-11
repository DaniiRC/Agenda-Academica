package com.example.agendaacademica.utils;

import android.os.SystemClock;

/**
 * Utilidad para prevenir el doble clic accidental sobre botones o elementos de navegación.
 * Bloquea cualquier nueva acción que ocurra en un intervalo inferior a {@link #MIN_INTERVAL} ms.
 */
public class ClickGuard {
    private static long lastClickTime = 0;
    private static final long MIN_INTERVAL = 800; // Intervalo mínimo en milisegundos entre clics válidos

    /**
     * Determina si un clic debe procesarse o debe descartarse por haber ocurrido demasiado pronto.
     *
     * @return {@code true} si el clic es válido, {@code false} si debe ignorarse.
     */

    public static boolean canClick() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastClickTime < MIN_INTERVAL) {
            return false;
        }
        lastClickTime = currentTime;
        return true;
    }
}
