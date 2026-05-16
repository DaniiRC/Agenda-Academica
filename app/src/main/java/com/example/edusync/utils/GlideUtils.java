package com.example.edusync.utils;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.edusync.R;
import com.example.edusync.network.RetrofitClient;

/**
 * Utilidad centralizada para la carga de imágenes con Glide.
 * Diferencia entre URLs externas (Google, etc.) y URLs del servidor propio,
 * aplicando estrategias de caché distintas según el origen.
 */
public class GlideUtils {

    /**
     * Carga la foto de perfil de un usuario en un ImageView circular.
     * Para URLs del servidor propio, fuerza la invalidación de caché mediante timestamp.
     *
     * @param context   Contexto necesario para Glide.
     * @param fotoUrl   URL de la imagen (puede ser relativa o absoluta).
     * @param imageView Vista de destino.
     */

    public static void cargarFotoPerfil(Context context, String fotoUrl, ImageView imageView) {
        if (fotoUrl != null && !fotoUrl.isEmpty() && !fotoUrl.equalsIgnoreCase("null")) {
            fotoUrl = fotoUrl.replace("\\", "/").replace(" ", "%20");
            if (fotoUrl.startsWith("http")) {
                // URL externa (p. ej. de Google OAuth): se usa la caché normal de Glide.
                Glide.with(context)
                        .load(fotoUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.mipmap.ic_launcher)
                        .circleCrop()
                        .into(imageView);
            } else {
                // URL del servidor propio: se añade un timestamp para forzar la recarga y evitar imágenes obsoletas.
                String fullUrl = RetrofitClient.BASE_URL + (fotoUrl.startsWith("/") ? fotoUrl.substring(1) : fotoUrl);
                String finalUrl = fullUrl + "?t=" + System.currentTimeMillis();
                Glide.with(context)
                        .load(finalUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.mipmap.ic_launcher)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(imageView);
            }
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    public static void cargarFotoGrupo(Context context, String fotoUrl, ImageView imageView) {
        if (fotoUrl != null && !fotoUrl.isEmpty() && !fotoUrl.equalsIgnoreCase("null")) {
            fotoUrl = fotoUrl.replace("\\", "/").replace(" ", "%20");
            String fullUrl = fotoUrl;
            if (!fotoUrl.startsWith("http")) {
                fullUrl = RetrofitClient.BASE_URL + (fotoUrl.startsWith("/") ? fotoUrl.substring(1) : fotoUrl);
            }

            String finalUrl = fullUrl + (fullUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();

            Glide.with(context)
                    .load(finalUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.mipmap.ic_launcher)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }
}
