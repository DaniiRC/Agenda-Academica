package com.example.edusync.activities;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.edusync.R;
import com.example.edusync.model.Calificacion;
import com.example.edusync.model.Usuario;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalificarAlumnosActivity extends BaseActivity {

    private RecyclerView rvCalificarAlumnos;
    private ProgressBar pbCalificar;
    private CalificarAdapter adapter;
    private Long eventoId;
    private Long grupoId;
    private ApiService apiService;

    private List<Usuario> participantesList = new ArrayList<>();
    private Map<Long, Double> calificacionesMap = new HashMap<>(); // usuarioId -> nota

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificar_alumnos);

        apiService = RetrofitClient.getApiService();
        eventoId = getIntent().getLongExtra("EVENTO_ID", -1);
        grupoId = getIntent().getLongExtra("GRUPO_ID", -1);

        Toolbar toolbar = findViewById(R.id.toolbarCalificar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvCalificarAlumnos = findViewById(R.id.rvCalificarAlumnos);
        rvCalificarAlumnos.setLayoutManager(new LinearLayoutManager(this));
        pbCalificar = findViewById(R.id.pbCalificar);

        cargarDatos();
    }

    private void cargarDatos() {
        pbCalificar.setVisibility(View.VISIBLE);
        // Primero, obtener participantes del grupo
        apiService.obtenerParticipantesPorGrupoId(grupoId).enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    participantesList = response.body();
                    cargarCalificaciones();
                } else {
                    pbCalificar.setVisibility(View.GONE);
                    Toast.makeText(CalificarAlumnosActivity.this, "Error al cargar participantes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                pbCalificar.setVisibility(View.GONE);
                Toast.makeText(CalificarAlumnosActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarCalificaciones() {
        apiService.obtenerCalificacionesPorEvento(eventoId).enqueue(new Callback<List<Calificacion>>() {
            @Override
            public void onResponse(Call<List<Calificacion>> call, Response<List<Calificacion>> response) {
                pbCalificar.setVisibility(View.GONE);
                calificacionesMap.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (Calificacion c : response.body()) {
                        calificacionesMap.put(c.getUsuarioId(), c.getNota());
                    }
                }
                configurarAdapter();
            }

            @Override
            public void onFailure(Call<List<Calificacion>> call, Throwable t) {
                pbCalificar.setVisibility(View.GONE);
                Toast.makeText(CalificarAlumnosActivity.this, "No se pudieron cargar notas previas", Toast.LENGTH_SHORT).show();
                configurarAdapter();
            }
        });
    }

    private void configurarAdapter() {
        adapter = new CalificarAdapter(participantesList, calificacionesMap, this::mostrarDialogoNota);
        rvCalificarAlumnos.setAdapter(adapter);
    }

    private void mostrarDialogoNota(Usuario usuario, Double notaActual) {
        EditText etNota = new EditText(this);
        etNota.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etNota.setHint("Ej: 8.5");

        if (notaActual != null) {
            etNota.setText(String.valueOf(notaActual));
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Calificar a " + usuario.getNombre())
                .setMessage("Introduce la nota:")
                .setView(etNota)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String val = etNota.getText().toString().replace(",", ".");
                    if (!val.isEmpty()) {
                        try {
                            double nota = Double.parseDouble(val);
                            if (nota < 0 || nota > 10) {
                                Toast.makeText(this, "La nota debe estar entre 0 y 10", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            guardarNota(usuario.getId(), nota);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Formato incorrecto", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarNota(Long usuarioId, Double nota) {
        pbCalificar.setVisibility(View.VISIBLE);
        apiService.guardarCalificacion(eventoId, usuarioId, nota).enqueue(new Callback<Calificacion>() {
            @Override
            public void onResponse(Call<Calificacion> call, Response<Calificacion> response) {
                pbCalificar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(CalificarAlumnosActivity.this, "Nota guardada", Toast.LENGTH_SHORT).show();
                    calificacionesMap.put(usuarioId, nota);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(CalificarAlumnosActivity.this, "Error al guardar nota", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Calificacion> call, Throwable t) {
                pbCalificar.setVisibility(View.GONE);
                Toast.makeText(CalificarAlumnosActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class CalificarAdapter extends RecyclerView.Adapter<CalificarAdapter.ViewHolder> {
        private final List<Usuario> usuarios;
        private final Map<Long, Double> calificaciones;
        private final OnCalificarClickListener listener;

        interface OnCalificarClickListener {
            void onClick(Usuario usuario, Double notaActual);
        }

        public CalificarAdapter(List<Usuario> usuarios, Map<Long, Double> calificaciones, OnCalificarClickListener listener) {
            this.usuarios = usuarios;
            this.calificaciones = calificaciones;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calificar_alumno, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Usuario u = usuarios.get(position);
            holder.tvNombre.setText(u.getNombre());
            holder.tvEmail.setText(u.getEmail());

            if (u.getFotoUrl() != null && !u.getFotoUrl().isEmpty()) {
                String fullUrl = u.getFotoUrl().startsWith("http") ?
                        u.getFotoUrl() : RetrofitClient.BASE_URL + u.getFotoUrl();

                Glide.with(holder.itemView.getContext())
                        .load(fullUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .circleCrop()
                        .into(holder.ivFoto);
            } else {
                holder.ivFoto.setImageResource(R.mipmap.ic_launcher_round);
            }

            Double nota = calificaciones.get(u.getId());
            if (nota != null) {
                holder.tvNota.setText(String.format(Locale.getDefault(), "%.2f", nota));
            } else {
                holder.tvNota.setText("Sin nota");
            }

            holder.llContainer.setOnClickListener(v -> listener.onClick(u, nota));
            holder.itemView.setOnClickListener(v -> listener.onClick(u, nota));
        }

        @Override
        public int getItemCount() {
            return usuarios.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvEmail, tvNota;
            ImageView ivFoto;
            LinearLayout llContainer;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tvNombreAlumno);
                tvEmail = itemView.findViewById(R.id.tvEmailAlumno);
                tvNota = itemView.findViewById(R.id.tvNotaAlumno);
                ivFoto = itemView.findViewById(R.id.ivFotoAlumno);
                llContainer = itemView.findViewById(R.id.llNotaContainer);
            }
        }
    }
}
