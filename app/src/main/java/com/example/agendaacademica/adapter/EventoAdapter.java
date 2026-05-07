package com.example.agendaacademica.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.agendaacademica.R;
import com.example.agendaacademica.model.Evento;
import java.util.List;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    public interface OnEventoClickListener {
        void onEventoClick(Evento evento, boolean isEditClick);
    }

    private List<Evento> listaEventos;
    private boolean isAdmin;
    private OnEventoClickListener listener;

    public EventoAdapter(List<Evento> listaEventos, boolean isAdmin, OnEventoClickListener listener) {
        this.listaEventos = listaEventos;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // IMPORTANTE: Asegúrate de apuntar al nuevo diseño de tarea
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = listaEventos.get(position);

        holder.tvItemTitulo.setText(evento.getTitulo());

        // Si tu evento tiene hora, ponla. Si no, usa la fecha.
        // Aquí podrías formatear la fecha a "15 Abr - 10:30" si quieres.
        holder.tvItemHora.setText(evento.getFecha());

        // APROVECHAMOS LA ASIGNATURA QUE VIENE DEL BACKEND
        if (evento.getAsignatura() != null) {
            holder.tvItemDesc.setText(evento.getAsignatura().getNombre());

            // Magia: Pintamos la barra lateral con el color que elegiste al crear la asignatura
            try {
                if (evento.getAsignatura().getColor() != null) {
                    holder.viewColorIndicador.setBackgroundColor(
                            android.graphics.Color.parseColor(evento.getAsignatura().getColor())
                    );
                } else {
                    holder.viewColorIndicador.setBackgroundColor(
                            holder.itemView.getContext().getResources().getColor(R.color.primary)
                    );
                }
            } catch (Exception e) {
                // Por si el color en la BD no es un HEX válido (ej. "rojo" en vez de "#FF0000")
                holder.viewColorIndicador.setBackgroundColor(
                        holder.itemView.getContext().getResources().getColor(R.color.primary)
                );
            }
        } else {
            holder.tvItemDesc.setText("Para mí");
            holder.viewColorIndicador.setBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.primary)
            );
        }

        // Lógica para Tipo de Evento
        String tipo = evento.getTipo();
        if (tipo == null) tipo = "Deberes";

        int colorPrimario = Color.parseColor("#3B82F6"); // Azul por defecto
        int colorFondoIcono = Color.parseColor("#223B82F6");
        int resIcono = android.R.drawable.ic_menu_recent_history;
        String textoEtiqueta = "DEBERES";

        if ("Examen".equalsIgnoreCase(tipo)) {
            colorPrimario = Color.parseColor("#EF4444");
            colorFondoIcono = Color.parseColor("#22EF4444");
            resIcono = android.R.drawable.ic_dialog_alert;
            textoEtiqueta = "EXAMEN";
        } else if ("Deberes".equalsIgnoreCase(tipo)) {
            colorPrimario = Color.parseColor("#3B82F6");
            colorFondoIcono = Color.parseColor("#223B82F6");
            resIcono = android.R.drawable.ic_menu_recent_history;
            textoEtiqueta = "DEBERES";
        } else if ("Proyecto".equalsIgnoreCase(tipo)) {
            colorPrimario = Color.parseColor("#F59E0B");
            colorFondoIcono = Color.parseColor("#22F59E0B");
            resIcono = android.R.drawable.ic_menu_agenda;
            textoEtiqueta = "PROYECTO";
        }

        holder.tvEtiquetaTipo.setText(textoEtiqueta);
        holder.tvEtiquetaTipo.setTextColor(colorPrimario);
        
        if (isAdmin) {
            holder.ivIconoTarea.setCardBackgroundColor(Color.parseColor("#22888888"));
            holder.ivTipoIcono.setImageResource(android.R.drawable.ic_menu_edit);
            holder.ivTipoIcono.setColorFilter(Color.parseColor("#757575"));
            
            holder.ivIconoTarea.setOnClickListener(v -> {
                if (listener != null) listener.onEventoClick(evento, true);
            });
        } else {
            holder.ivIconoTarea.setCardBackgroundColor(colorFondoIcono);
            holder.ivTipoIcono.setImageResource(resIcono);
            holder.ivTipoIcono.setColorFilter(colorPrimario);
            holder.ivIconoTarea.setOnClickListener(null); // Quitar listener
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventoClick(evento, false);
        });
    }

    @Override
    public int getItemCount() {
        return listaEventos.size();
    }

    public void updateData(List<Evento> nuevosEventos) {
        this.listaEventos = nuevosEventos;
        notifyDataSetChanged();
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    public static class EventoViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemTitulo, tvItemDesc, tvItemHora, tvEtiquetaTipo;
        View viewColorIndicador;
        MaterialCardView ivIconoTarea;
        ImageView ivTipoIcono;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemTitulo = itemView.findViewById(R.id.tvItemTitulo);
            tvItemDesc = itemView.findViewById(R.id.tvItemDesc);
            tvItemHora = itemView.findViewById(R.id.tvItemHora);
            viewColorIndicador = itemView.findViewById(R.id.viewColorIndicador);
            tvEtiquetaTipo = itemView.findViewById(R.id.tvEtiquetaTipo);
            ivIconoTarea = itemView.findViewById(R.id.ivIconoTarea);
            ivTipoIcono = itemView.findViewById(R.id.ivTipoIcono);
        }
    }
}