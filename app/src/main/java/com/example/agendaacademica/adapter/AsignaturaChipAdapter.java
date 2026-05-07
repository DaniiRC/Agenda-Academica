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
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AsignaturaChipAdapter extends RecyclerView.Adapter<AsignaturaChipAdapter.ChipViewHolder> {

    private List<Asignatura> listaAsignaturas;
    private OnChipClickListener listener;
    private int selectedPosition = -1;

    // Interfaz para enterarnos en la Activity de qué chip se ha pulsado
    public interface OnChipClickListener {
        void onChipClick(Asignatura asignatura);
    }

    public AsignaturaChipAdapter(List<Asignatura> listaAsignaturas, OnChipClickListener listener) {
        this.listaAsignaturas = listaAsignaturas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Asegúrate de que este sea el nombre real de tu layout para el chip
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asignatura_chip, parent, false);
        return new ChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        Asignatura asignatura = listaAsignaturas.get(position);

        // 1. Ponemos el nombre de la materia
        holder.tvNombreChip.setText(asignatura.getNombre());

        boolean isSelected = (position == selectedPosition);

        try {
            if (asignatura.getColor() != null && !asignatura.getColor().isEmpty()) {
                int parsedColor = Color.parseColor(asignatura.getColor());

                if (isSelected) {
                    holder.cardChip.setCardBackgroundColor(parsedColor);
                    holder.tvNombreChip.setTextColor(Color.WHITE);
                    holder.cardChip.setStrokeWidth(0);
                } else {
                    holder.cardChip.setCardBackgroundColor(Color.TRANSPARENT);
                    holder.tvNombreChip.setTextColor(parsedColor);
                    holder.cardChip.setStrokeColor(parsedColor);
                    holder.cardChip.setStrokeWidth(3);
                }
            }
        } catch (Exception e) {
            // Ignorar
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            if (selectedPosition == holder.getAdapterPosition()) {
                // Deselect
                selectedPosition = -1;
                if (listener != null) listener.onChipClick(null);
            } else {
                // Select
                selectedPosition = holder.getAdapterPosition();
                if (listener != null) listener.onChipClick(asignatura);
            }
            
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return listaAsignaturas != null ? listaAsignaturas.size() : 0;
    }

    public static class ChipViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreChip;
        MaterialCardView cardChip;

        public ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            // ¡OJO! Revisa que estos IDs coincidan con los que tengas en item_asignatura_chip.xml
            tvNombreChip = itemView.findViewById(R.id.tvNombreChip);
            cardChip = itemView.findViewById(R.id.cardChip);
        }
    }
}