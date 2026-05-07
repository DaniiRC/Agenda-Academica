package com.example.agendaacademica.utils;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.agendaacademica.R;
import com.example.agendaacademica.network.RetrofitClient;

public class GlideUtils {

    public static void cargarFotoPerfil(Context context, String fotoUrl, ImageView imageView) {
        if (fotoUrl != null && !fotoUrl.isEmpty() && !fotoUrl.equalsIgnoreCase("null")) {
            fotoUrl = fotoUrl.replace("\\", "/").replace(" ", "%20");
            if (fotoUrl.startsWith("http")) {
                // URL externa (Google, etc.) — usar caché normal de Glide
                Glide.with(context)
                        .load(fotoUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.mipmap.ic_launcher)
                        .circleCrop()
                        .into(imageView);
            } else {
                // URL del servidor propio — añadir timestamp para forzar refresco
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
                    .error(R.mipmap.ic_launcher) // Debería ser una imagen por defecto de grupo
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }
}
