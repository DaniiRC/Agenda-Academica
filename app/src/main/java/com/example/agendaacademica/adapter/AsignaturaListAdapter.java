package com.example.agendaacademica.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaacademica.R;
import com.example.agendaacademica.model.Asignatura;

import java.util.List;

public class AsignaturaListAdapter extends RecyclerView.Adapter<AsignaturaListAdapter.ViewHolder> {

    private List<Asignatura> asignaturas;
    private boolean isAdmin;
    private OnAsignaturaActionListener listener;

    public interface OnAsignaturaActionListener {
        void onAsignaturaClick(Asignatura asignatura);
        void onAsignaturaDelete(Asignatura asignatura);
    }

    public AsignaturaListAdapter(List<Asignatura> asignaturas, boolean isAdmin, OnAsignaturaActionListener listener) {
        this.asignaturas = asignaturas;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asignatura_full, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asignatura asignatura = asignaturas.get(position);
        holder.tvNombre.setText(asignatura.getNombre());
        
        if (asignatura.getColor() != null && !asignatura.getColor().isEmpty()) {
            try {
                holder.viewColor.setBackgroundColor(Color.parseColor(asignatura.getColor()));
            } catch (Exception e) {
                holder.viewColor.setBackgroundResource(R.color.primary);
            }
        }

        if (isAdmin) {
            holder.btnEliminar.setVisibility(View.VISIBLE);
            holder.btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onAsignaturaDelete(asignatura);
            });
        } else {
            holder.btnEliminar.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAsignaturaClick(asignatura);
        });
    }

    @Override
    public int getItemCount() {
        return asignaturas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        View viewColor;
        View btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreAsignatura);
            viewColor = itemView.findViewById(R.id.viewColorAsignatura);
            btnEliminar = itemView.findViewById(R.id.btnEliminarAsignatura);
        }
    }
}
