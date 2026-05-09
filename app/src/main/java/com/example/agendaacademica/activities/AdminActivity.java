package com.example.agendaacademica.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.model.Usuario;
import com.example.agendaacademica.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends BaseActivity {

    private RecyclerView rvUsuarios;
    private ProgressBar pbAdmin;
    private UsuariosAdapter adapter;
    private List<Usuario> listaUsuarios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbarAdmin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvUsuarios = findViewById(R.id.rvUsuarios);
        pbAdmin = findViewById(R.id.pbAdmin);

        findViewById(R.id.btnLogoutAdmin).setOnClickListener(v -> {
            new SessionManager(this).cerrarSesion();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsuariosAdapter(listaUsuarios);
        rvUsuarios.setAdapter(adapter);

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        pbAdmin.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().obtenerTodosLosUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                pbAdmin.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    listaUsuarios.clear();
                    listaUsuarios.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AdminActivity.this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                pbAdmin.setVisibility(View.GONE);
                Toast.makeText(AdminActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarUsuario(Usuario u) {
        if (u.getEmail().equals(new SessionManager(this).obtenerEmailUsuario())) {
            Toast.makeText(this, "No puedes eliminarte a ti mismo", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Estás seguro de que quieres eliminar a " + u.getNombre() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    pbAdmin.setVisibility(View.VISIBLE);
                    RetrofitClient.getApiService().eliminarUsuario(u.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            pbAdmin.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminActivity.this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                                cargarUsuarios();
                            } else {
                                Toast.makeText(AdminActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            pbAdmin.setVisibility(View.GONE);
                            Toast.makeText(AdminActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void editarUsuario(Usuario u) {
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_user_admin, null);
        EditText etNombre = v.findViewById(R.id.etEditNombre);
        EditText etEmail = v.findViewById(R.id.etEditEmail);
        EditText etPass = v.findViewById(R.id.etEditPassword);
        android.widget.Spinner spRol = v.findViewById(R.id.spEditRol);

        etNombre.setText(u.getNombre());
        etEmail.setText(u.getEmail());
        
        String[] roles = {"USER", "ADMIN"};
        android.widget.ArrayAdapter<String> adapterRol = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapterRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRol.setAdapter(adapterRol);
        spRol.setSelection(u.getRol().equals("ADMIN") ? 1 : 0);

        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String pass = etPass.getText().toString().trim();
                    u.setNombre(etNombre.getText().toString().trim());
                    u.setEmail(etEmail.getText().toString().trim());
                    u.setRol(spRol.getSelectedItem().toString());
                    if (!pass.isEmpty()) u.setPassword(pass);
                    else u.setPassword(null); // El backend ignorará si es null

                    pbAdmin.setVisibility(View.VISIBLE);
                    RetrofitClient.getApiService().actualizarUsuarioAdmin(u.getId(), u).enqueue(new Callback<Usuario>() {
                        @Override
                        public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                            pbAdmin.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminActivity.this, "Usuario actualizado", Toast.LENGTH_SHORT).show();
                                cargarUsuarios();
                            }
                        }

                        @Override
                        public void onFailure(Call<Usuario> call, Throwable t) {
                            pbAdmin.setVisibility(View.GONE);
                            Toast.makeText(AdminActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.ViewHolder> {
        private List<Usuario> usuarios;

        UsuariosAdapter(List<Usuario> usuarios) {
            this.usuarios = usuarios;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Usuario u = usuarios.get(position);
            holder.tvName.setText(u.getNombre());
            holder.tvEmail.setText(u.getEmail());
            holder.tvRol.setText(u.getRol());

            if (u.getFotoUrl() != null) {
                Glide.with(holder.itemView.getContext())
                        .load(RetrofitClient.BASE_URL.substring(0, RetrofitClient.BASE_URL.length()-1) + u.getFotoUrl())
                        .placeholder(R.drawable.ic_people)
                        .into(holder.ivAvatar);
            }

            holder.btnDelete.setOnClickListener(v -> eliminarUsuario(u));
            holder.btnEdit.setOnClickListener(v -> editarUsuario(u));
        }

        @Override
        public int getItemCount() {
            return usuarios.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRol;
            ImageView ivAvatar;
            View btnEdit, btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvEmail = itemView.findViewById(R.id.tvUserEmail);
                tvRol = itemView.findViewById(R.id.tvUserRol);
                ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
                btnEdit = itemView.findViewById(R.id.btnEditUser);
                btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            }
        }
    }
}
