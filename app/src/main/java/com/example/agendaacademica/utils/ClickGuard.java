package com.example.agendaacademica.utils;

import android.os.SystemClock;

/**
 * Utilidad para prevenir el "doble clic" accidental que abre varias pantallas
 * o dispara acciones repetitivas de forma errática.
 */
public class ClickGuard {
    private static long lastClickTime = 0;
    private static final long MIN_INTERVAL = 800; // milisegundos

    public static boolean canClick() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastClickTime < MIN_INTERVAL) {
            return false;
        }
        lastClickTime = currentTime;
        return true;
    }
}
