package com.example.edusync.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.edusync.R;
import com.example.edusync.model.Evento;
import java.util.List;

/**
 * Adaptador para mostrar eventos académicos dentro del contexto de una clase (GrupoFragment).
 * Diferencia visualmente entre eventos propios (modo alumno) y editables (modo administrador).
 * Resuelve el color de la barra lateral usando el color definido en la asignatura correspondiente.
 */
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = listaEventos.get(position);

        holder.tvItemTitulo.setText(evento.getTitulo());

        // Formatea la fecha del evento de "yyyy-MM-dd" a "dd MMM" según el idioma del dispositivo.
        try {
            java.text.SimpleDateFormat sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.text.SimpleDateFormat sdfOut = new java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault());
            holder.tvItemHora.setText(sdfOut.format(sdfIn.parse(evento.getFecha())));
        } catch (Exception e) {
            holder.tvItemHora.setText(evento.getFecha());
        }

        // Muestra el nombre de la asignatura en el subtexto y colorea la barra lateral con su color de marca.
        if (evento.getAsignatura() != null) {
            holder.tvItemDesc.setText(evento.getAsignatura().getNombre());

            // El color de la barra proviene del campo color de la asignatura (formato HEX, ej: "#3B82F6").
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
                // Si el valor almacenado no es un color HEX válido, se usa el color primario de la aplicación.
                holder.viewColorIndicador.setBackgroundColor(
                        holder.itemView.getContext().getResources().getColor(R.color.primary)
                );
            }
        } else {
            holder.tvItemDesc.setText(holder.itemView.getContext().getString(R.string.solo_para_mi));
            holder.viewColorIndicador.setBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.primary)
            );
        }

        // Determina los colores e icono según el tipo de evento.
        String tipo = evento.getTipo();
        if (tipo == null) tipo = "Deberes";

        int colorPrimario = Color.parseColor("#3B82F6"); // Azul (valor por defecto para tipo Deberes)
        int colorFondoIcono = Color.parseColor("#223B82F6");
        int resIcono = android.R.drawable.ic_menu_recent_history;
        String textoEtiqueta = holder.itemView.getContext().getString(R.string.deberes_mayus);

        if ("Examen".equalsIgnoreCase(tipo)) {
            colorPrimario = Color.parseColor("#EF4444");
            colorFondoIcono = Color.parseColor("#22EF4444");
            resIcono = android.R.drawable.ic_dialog_alert;
            textoEtiqueta = holder.itemView.getContext().getString(R.string.examen_mayus);
        } else if ("Deberes".equalsIgnoreCase(tipo)) {
            colorPrimario = Color.parseColor("#3B82F6");
            colorFondoIcono = Color.parseColor("#223B82F6");
            resIcono = android.R.drawable.ic_menu_recent_history;
            textoEtiqueta = holder.itemView.getContext().getString(R.string.deberes_mayus);
        } else if ("Proyecto".equalsIgnoreCase(tipo)) {
            colorPrimario = Color.parseColor("#F59E0B");
            colorFondoIcono = Color.parseColor("#22F59E0B");
            resIcono = android.R.drawable.ic_menu_agenda;
            textoEtiqueta = holder.itemView.getContext().getString(R.string.proyecto_mayus);
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
            holder.ivIconoTarea.setOnClickListener(null); // Sin acción de edición para alumnos.
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
