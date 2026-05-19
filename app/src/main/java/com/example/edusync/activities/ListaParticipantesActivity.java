package com.example.edusync.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.edusync.R;
import com.example.edusync.model.Grupo;
import com.example.edusync.model.Usuario;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad que lista todos los participantes integrados en un grupo académico específico.
 * Distingue visualmente entre el administrador/profesor del grupo y los alumnos asociados,
 * recuperando la información correspondiente de forma asíncrona a través del servicio API.
 */
public class ListaParticipantesActivity extends BaseActivity {

    private RecyclerView rvParticipantes;
    private ParticipantesAdapter adapter;
    private String codigoGrupo;
    private Long profesorId;
    private ApiService apiService;

    /**
     * Inicializa la actividad, recupera los datos pasados por el Intent,
     * configura la barra de herramientas y prepara la lista.
     *
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_participantes);

        apiService = RetrofitClient.getApiService();
        codigoGrupo = getIntent().getStringExtra("GRUPO_CODIGO");
        profesorId = getIntent().getLongExtra("PROFESOR_ID", -1);

        Toolbar toolbar = findViewById(R.id.toolbarParticipantes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvParticipantes = findViewById(R.id.rvParticipantes);
        rvParticipantes.setLayoutManager(new LinearLayoutManager(this));

        // Para asegurar que el profesorId es correcto, lo buscamos de nuevo del grupo
        recuperarInfoGrupoYParticipantes();
    }

    /**
     * Realiza una consulta asíncrona a la API para obtener la información detallada del grupo,
     * especialmente para asegurar el identificador correcto del profesor administrador
     * antes de proceder con la carga de la lista completa de participantes.
     */
    private void recuperarInfoGrupoYParticipantes() {
        apiService.buscarGrupoPorCodigo(codigoGrupo).enqueue(new Callback<Grupo>() {
            @Override
            public void onResponse(Call<Grupo> call, Response<Grupo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Grupo grupo = response.body();
                    if (grupo.getProfesor() != null) {
                        profesorId = grupo.getProfesor().getId();
                    }
                }
                // Independientemente de si encontramos al profesor, cargamos la lista
                cargarParticipantes();
            }

            @Override
            public void onFailure(Call<Grupo> call, Throwable t) {
                cargarParticipantes();
            }
        });
    }

    /**
     * Recupera asíncronamente de la API la lista de participantes inscritos en el grupo
     * y los vincula al adaptador del RecyclerView para su visualización.
     */
    private void cargarParticipantes() {
        apiService.obtenerParticipantes(codigoGrupo).enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new ParticipantesAdapter(response.body(), profesorId);
                    rvParticipantes.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                Toast.makeText(ListaParticipantesActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Adaptador para el RecyclerView que renderiza la lista de participantes.
     * Gestiona las etiquetas visuales diferenciando roles de administrador (ADMIN) y alumnos (ALUMNO).
     */
    private static class ParticipantesAdapter extends RecyclerView.Adapter<ParticipantesAdapter.ViewHolder> {
        private List<Usuario> usuarios;
        private Long adminId;

        /**
         * Constructor del adaptador de participantes.
         *
         * @param usuarios Lista de usuarios del grupo.
         * @param adminId  Identificador del profesor/administrador del grupo.
         */
        public ParticipantesAdapter(List<Usuario> usuarios, Long adminId) {
            this.usuarios = usuarios;
            this.adminId = adminId;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participante, parent, false);
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
                        .placeholder(R.drawable.ic_launcher_background)
                        .circleCrop()
                        .into(holder.ivFoto);
            } else {
                holder.ivFoto.setImageResource(R.mipmap.ic_launcher_round);
            }

            holder.tvRol.setVisibility(View.VISIBLE);
            
            // Comparación robusta para discernir el rol del participante en el grupo
            if (adminId != null && adminId != -1 && u.getId() != null && u.getId().equals(adminId)) {
                holder.tvRol.setText("ADMIN");
                holder.tvRol.setBackgroundTintList(ColorStateList.valueOf(0xFFF0F9FF));
                holder.tvRol.setTextColor(0xFF0369A1);
            } else {
                holder.tvRol.setText("ALUMNO");
                holder.tvRol.setBackgroundTintList(ColorStateList.valueOf(0xFFF0FDF4));
                holder.tvRol.setTextColor(0xFF166534);
            }
        }

        @Override
        public int getItemCount() {
            return usuarios.size();
        }

        /**
         * Contenedor de vistas de los elementos individuales de la lista de participantes.
         */
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvEmail, tvRol;
            ImageView ivFoto;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tvNombreParticipante);
                tvEmail = itemView.findViewById(R.id.tvEmailParticipante);
                ivFoto = itemView.findViewById(R.id.ivFotoParticipante);
                tvRol = itemView.findViewById(R.id.tvRolParticipante);
            }
        }
    }
}
