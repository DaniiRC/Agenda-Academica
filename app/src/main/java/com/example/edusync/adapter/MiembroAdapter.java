package com.example.edusync.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.edusync.R;
import com.example.edusync.model.Usuario;
import com.example.edusync.network.RetrofitClient;
import java.util.List;

public class MiembroAdapter extends RecyclerView.Adapter<MiembroAdapter.MiembroViewHolder> {

    private List<Usuario> listaMiembros;
    private Long profesorId;

    public MiembroAdapter(List<Usuario> listaMiembros, Long profesorId) {
        this.listaMiembros = listaMiembros;
        this.profesorId = profesorId;
    }

    @NonNull
    @Override
    public MiembroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_miembro, parent, false);
        return new MiembroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MiembroViewHolder holder, int position) {
        Usuario usuario = listaMiembros.get(position);
        holder.tvNombre.setText(usuario.getNombre());
        
        boolean isAdmin = usuario.getId() != null && usuario.getId().equals(profesorId);
        if (isAdmin) {
            holder.tvRol.setText("Administrador");
            holder.ivAdmin.setVisibility(View.VISIBLE);
        } else {
            holder.tvRol.setText("Alumno");
            holder.ivAdmin.setVisibility(View.GONE);
        }

        com.example.edusync.utils.GlideUtils.cargarFotoPerfil(holder.itemView.getContext(), usuario.getFotoUrl(), holder.ivFoto);
    }

    @Override
    public int getItemCount() {
        return listaMiembros.size();
    }

    public static class MiembroViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto, ivAdmin;
        TextView tvNombre, tvRol;

        public MiembroViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoMiembro);
            tvNombre = itemView.findViewById(R.id.tvNombreMiembro);
            tvRol = itemView.findViewById(R.id.tvRolMiembro);
            ivAdmin = itemView.findViewById(R.id.ivAdminStatus);
        }
    }
}
