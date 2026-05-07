package com.example.agendaacademica.adapter;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaacademica.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

public class CalendarioAdapter extends RecyclerView.Adapter<CalendarioAdapter.CalendarioViewHolder> {

    private final ArrayList<String> diasDelMes;
    private final OnItemListener onItemListener;
    private final Map<String, String> coloresPorDia;
    private final int diaSeleccionado;
    private final int mesMostrado;
    private final int anioMostrado;

    public CalendarioAdapter(ArrayList<String> diasDelMes, Map<String, String> coloresPorDia, int diaSeleccionado, int mes, int anio, OnItemListener onItemListener) {
        this.diasDelMes = diasDelMes;
        this.coloresPorDia = coloresPorDia;
        this.diaSeleccionado = diaSeleccionado;
        this.mesMostrado = mes;
        this.anioMostrado = anio;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public CalendarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_celda_calendario, parent, false);
        return new CalendarioViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarioViewHolder holder, int position) {
        String dia = diasDelMes.get(position);
        holder.tvDiaCalendario.setText(dia);

        if (!dia.isEmpty()) {
            int diaInt = Integer.parseInt(dia);

            // Comprobar si es el día de hoy real
            boolean esHoy = (diaInt == java.time.LocalDate.now().getDayOfMonth() &&
                    mesMostrado == java.time.LocalDate.now().getMonthValue() &&
                    anioMostrado == java.time.LocalDate.now().getYear());

            // Comprobar si es el día que el usuario tiene seleccionado (clicado)
            boolean esSeleccionado = (diaInt == diaSeleccionado);

            // 1. Círculo azul para el día seleccionado
            if (esSeleccionado) {
                holder.viewSeleccion.setVisibility(View.VISIBLE);
                holder.tvDiaCalendario.setTextColor(android.graphics.Color.WHITE);
                holder.tvDiaCalendario.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.viewSeleccion.setVisibility(View.GONE);

                // 2. Color especial para "Hoy" si no está seleccionado
                if (esHoy) {
                    holder.tvDiaCalendario.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.color_secondary));
                    holder.tvDiaCalendario.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    holder.tvDiaCalendario.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_main));
                    holder.tvDiaCalendario.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }

            // 3. Puntito indicador de eventos (debajo del número)
            if (coloresPorDia.containsKey(dia)) {
                holder.puntoEvento.setVisibility(View.VISIBLE);
                String colorHex = coloresPorDia.get(dia);
                try {
                    holder.puntoEvento.getBackground().setTint(android.graphics.Color.parseColor(colorHex != null ? colorHex : "#818CF8"));
                } catch (Exception e) {
                    holder.puntoEvento.getBackground().setTint(android.graphics.Color.parseColor("#818CF8"));
                }
            } else {
                holder.puntoEvento.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.viewSeleccion.setVisibility(View.GONE);
            holder.puntoEvento.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return diasDelMes.size();
    }

    public interface OnItemListener {
        void onItemClick(int position, String diaText);
    }

    public static class CalendarioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final TextView tvDiaCalendario;
        public final View puntoEvento;
        public final View viewSeleccion;
        private final OnItemListener onItemListener;

        public CalendarioViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            tvDiaCalendario = itemView.findViewById(R.id.tvDiaCalendario);
            puntoEvento = itemView.findViewById(R.id.puntoEvento);
            viewSeleccion = itemView.findViewById(R.id.viewSeleccion);
            this.onItemListener = onItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemListener.onItemClick(getAdapterPosition(), tvDiaCalendario.getText().toString());
        }
    }
}