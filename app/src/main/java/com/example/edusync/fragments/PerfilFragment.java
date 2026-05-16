package com.example.edusync.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.example.edusync.activities.AjustesActivity;
import com.example.edusync.activities.EditarPerfilActivity;
import com.example.edusync.activities.LoginActivity;
import com.example.edusync.model.Asignatura;
import com.example.edusync.model.Evento;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento que muestra el perfil del usuario en sesión.
 * Presenta estadísticas de rendimiento (tareas completadas, sesiones de enfoque, tiempo invertido)
 * y calcula la racha de días con actividad académica de la semana actual.
 */
public class PerfilFragment extends Fragment {

    private SessionManager session;
    private ImageView imgPerfil;
    private TextView tvNombre, tvEmail;
    private TextView tvTasksCount, tvTimeCount, tvClassesCount, tvProjectsCount;
    private TextView tvStreakTitle, tvStreakDesc;
    private View[] dayViews;
    private ImageView[] dayIcons;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        session = new SessionManager(requireContext());

        ImageButton btnSettings = view.findViewById(R.id.btn_settings);
        imgPerfil = view.findViewById(R.id.img_perfil);
        FloatingActionButton btnEditarPerfil = view.findViewById(R.id.btn_editar_perfil);
        tvNombre = view.findViewById(R.id.tv_nombre);
        tvEmail = view.findViewById(R.id.tv_email);
        
        tvTasksCount = view.findViewById(R.id.tv_tasks_count);
        tvTimeCount = view.findViewById(R.id.tv_time_count);
        tvClassesCount = view.findViewById(R.id.tv_classes_count);
        tvProjectsCount = view.findViewById(R.id.tv_projects_count);

        tvStreakTitle = view.findViewById(R.id.tv_streak_title);
        tvStreakDesc = view.findViewById(R.id.tv_streak_desc);

        initStreakDays(view);
        
        MaterialButton btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion);
        swipeRefresh = view.findViewById(R.id.swipeRefreshPerfil);
        swipeRefresh.setOnRefreshListener(this::cargarEstadisticas);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AjustesActivity.class);
            startActivity(intent);
        });

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditarPerfilActivity.class);
            startActivity(intent);
        });

        btnCerrarSesion.setOnClickListener(v -> {
            session.cerrarSesion();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        actualizarUI();
        cargarEstadisticas();

        return view;
    }

    private void actualizarUI() {
        tvNombre.setText(session.obtenerNombreUsuario());
        tvEmail.setText(session.obtenerEmailUsuario());

        com.example.edusync.utils.GlideUtils.cargarFotoPerfil(
                requireContext(),
                session.obtenerFotoUsuario(),
                imgPerfil
        );
    }

    private void cargarEstadisticas() {
        Long usuarioId = session.getUserId();
        ApiService apiService = RetrofitClient.getApiService();

        // Petición 1: grupos activos del usuario (para el contador de "Clases activas").
        apiService.obtenerGruposDeUsuario(usuarioId).enqueue(new Callback<List<com.example.edusync.model.Grupo>>() {
            @Override
            public void onResponse(Call<List<com.example.edusync.model.Grupo>> call, Response<List<com.example.edusync.model.Grupo>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    int numGrupos = response.body().size();
                    tvClassesCount.setText(String.valueOf(numGrupos));
                }
            }

            @Override
            public void onFailure(Call<List<com.example.edusync.model.Grupo>> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (!isAdded()) return;
                tvClassesCount.setText("0");
            }
        });

        // Petición 2: eventos del usuario (para calcular tareas completadas, tiempo y sesiones de enfoque).
        apiService.obtenerEventosUsuario(usuarioId).enqueue(new Callback<List<Evento>>() {
            @Override
            public void onResponse(Call<List<Evento>> call, Response<List<Evento>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (!isAdded()) return; // Seguridad: evitar actualizar vistas si el fragmento fue destruido.

                if (response.isSuccessful() && response.body() != null) {
                    List<Evento> eventos = response.body();

                    int tareasCompletadas = 0;
                    int sesionesEnfoque = 0;
                    long tiempoTotalMilis = 0;

                    for (Evento e : eventos) {
                        // El tiempo invertido se acumula independientemente de si la tarea está completada.
                        if (e.getTiempoInvertidoFocus() != null) {
                            tiempoTotalMilis += e.getTiempoInvertidoFocus();
                            // Cualquier evento con tiempo invertido mayor a cero cuenta como sesión de enfoque.
                            if (e.getTiempoInvertidoFocus() > 0) {
                                sesionesEnfoque++;
                            }
                        }

                        // Solo se contabiliza como tarea completada si su estado es "completado".
                        if (e.isCompletado()) {
                            String tipo = e.getTipo();
                            if (tipo != null) {
                                String t = tipo.toLowerCase();
                                if (t.contains("deber") || t.contains("tarea") || t.contains("estudio") || t.contains("proyecto")) {
                                    tareasCompletadas++;
                                }
                            }
                        }
                    }

                    tvTasksCount.setText(String.valueOf(tareasCompletadas));
                    tvProjectsCount.setText(String.valueOf(sesionesEnfoque));
                    
                    // Formato dinámico del tiempo: segundos → minutos → horas según la magnitud.
                    String tiempoFormateado;
                    long segundosTotales = tiempoTotalMilis / 1000;
                    long minutosTotales = segundosTotales / 60;

                    if (segundosTotales < 60) {
                        tiempoFormateado = segundosTotales + "s";
                    } else if (minutosTotales < 60) {
                        tiempoFormateado = minutosTotales + "m";
                    } else {
                        double horas = minutosTotales / 60.0;
                        if (horas % 1 == 0) tiempoFormateado = (int)horas + "h";
                        else tiempoFormateado = String.format(java.util.Locale.US, "%.1fh", horas);
                    }
                    tvTimeCount.setText(tiempoFormateado);

                    calcularRacha(eventos);
                }
            }

            @Override
            public void onFailure(Call<List<Evento>> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (!isAdded()) return;
                tvTasksCount.setText("0");
                tvProjectsCount.setText("0");
                tvTimeCount.setText("0s");
                tvStreakTitle.setText(getString(R.string.sin_racha));
            }
        });
    }

    private void initStreakDays(View view) {
        ViewGroup layoutDays = view.findViewById(R.id.layout_days);
        dayViews = new View[7];
        dayIcons = new ImageView[7];
        int[] letterRes = {
                R.string.lunes_sigla, R.string.martes_sigla, R.string.miercoles_sigla,
                R.string.jueves_sigla, R.string.viernes_sigla, R.string.sabado_sigla,
                R.string.domingo_sigla
        };

        for (int i = 0; i < 7; i++) {
            View dayItem = layoutDays.getChildAt(i);
            TextView tvLetter = dayItem.findViewById(R.id.tv_day_letter);
            tvLetter.setText(getString(letterRes[i]));
            dayViews[i] = dayItem.findViewById(R.id.view_day_bg);
            dayIcons[i] = dayItem.findViewById(R.id.iv_day_status);
        }
    }

    private void calcularRacha(List<Evento> eventos) {
        if (!isAdded()) return;

        Set<String> diasConActividad = new HashSet<>();
        for (Evento e : eventos) {
            if (e.isCompletado() && e.getFecha() != null) {
                diasConActividad.add(e.getFecha());
            }
        }

        // Calcula la racha retrocediendo desde hoy (o ayer si hoy aún no hay actividad).
        int rachaContinua = 0;
        Calendar cal = Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        String hoyStr = sdf.format(cal.getTime());

        Calendar tempCal = Calendar.getInstance();
        if (!diasConActividad.contains(hoyStr)) {
            tempCal.add(Calendar.DAY_OF_YEAR, -1);
        }
        
        while (diasConActividad.contains(sdf.format(tempCal.getTime()))) {
            rachaContinua++;
            tempCal.add(Calendar.DAY_OF_YEAR, -1);
        }


        Calendar calSemana = Calendar.getInstance();
        // Calcula el lunes de la semana actual para iterar los 7 días.
        int dayOfWeek = calSemana.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - Calendar.MONDAY);
        calSemana.add(Calendar.DAY_OF_YEAR, -daysToSubtract);

        for (int i = 0; i < 7; i++) {
            String fechaStr = sdf.format(calSemana.getTime());
            boolean huboActividad = diasConActividad.contains(fechaStr);

            // Marca el día como "perdido" si no hubo actividad pero el día anterior sí la tuvo.
            boolean esDiaPerdido = false;
            if (!huboActividad && calSemana.before(Calendar.getInstance())) {
                Calendar prevDay = (Calendar) calSemana.clone();
                prevDay.add(Calendar.DAY_OF_YEAR, -1);
                if (diasConActividad.contains(sdf.format(prevDay.getTime()))) {
                    esDiaPerdido = true;
                }
            }

            dayViews[i].setBackgroundResource(R.drawable.bg_circle_streak);
            dayIcons[i].setVisibility(View.GONE);

            if (huboActividad) {
                dayViews[i].setBackgroundResource(R.drawable.bg_circle_streak_active);
                dayIcons[i].setVisibility(View.VISIBLE);
                dayIcons[i].setImageResource(R.drawable.ic_check);
                dayIcons[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            } else if (esDiaPerdido) {
                dayViews[i].setVisibility(View.VISIBLE);
                dayIcons[i].setVisibility(View.VISIBLE);
                dayIcons[i].setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                dayIcons[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
            calSemana.add(Calendar.DAY_OF_YEAR, 1);
        }

        tvStreakTitle.setText(getString(R.string.racha_formato, rachaContinua));
        
        // El mensaje de descripción varía según si la racha es alta, baja o se ha rompido.
        Calendar ayer = Calendar.getInstance();
        ayer.add(Calendar.DAY_OF_YEAR, -1);
        if (rachaContinua == 0 && !diasConActividad.contains(sdf.format(ayer.getTime())) && !diasConActividad.contains(hoyStr)) {
             tvStreakDesc.setText("¡Racha perdida! Vuelve a empezar.");
        } else if (rachaContinua == 0) {
             tvStreakDesc.setText("¡Racha perdida! Vuelve a empezar.");
        } else if (rachaContinua >= 5) {
            tvStreakDesc.setText(R.string.streak_increible);
        } else {
            tvStreakDesc.setText(R.string.streak_buen_trabajo);
        }
        if (rachaContinua == 0) {
            tvStreakDesc.setText("¡Racha perdida! Vuelve a empezar.");
        } else if (rachaContinua >= 5) {
            tvStreakDesc.setText(R.string.streak_increible);
        } else {
            tvStreakDesc.setText(R.string.streak_buen_trabajo);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarUI();
        cargarEstadisticas();
    }
}
