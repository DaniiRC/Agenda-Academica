package com.example.agendaacademica.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaacademica.R;
import com.example.agendaacademica.fragments.AgendaFragment.EventoLocal;
import com.example.agendaacademica.activities.DetalleEventoActivity;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TareaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_HEADER = 1;

    private final List<Object> items = new java.util.ArrayList<>();
    private final OnTareaLongClickListener longClickListener;
    private boolean mostrarHeaders = true;

    public interface OnTareaLongClickListener {
        void onTareaLongClick(EventoLocal evento, View view);
        void onTareaDeleteClick(EventoLocal evento);
    }

    public TareaAdapter(List<EventoLocal> eventos, OnTareaLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
        procesarLista(eventos);
    }

    public void setMostrarHeaders(boolean mostrar) {
        this.mostrarHeaders = mostrar;
    }

    public void setTareas(List<EventoLocal> nuevasTareas) {
        procesarLista(nuevasTareas);
        notifyDataSetChanged();
    }

    private void procesarLista(List<EventoLocal> eventos) {
        items.clear();
        LocalDate hoy = LocalDate.now();
        boolean headerActualesMostrado = false;
        boolean headerPasadasMostrado = false;

        for (EventoLocal e : eventos) {
            boolean esPasada = e.fecha.isBefore(hoy);
            
            if (mostrarHeaders) {
                if (!esPasada && !headerActualesMostrado) {
                    items.add("Tareas Actuales");
                    headerActualesMostrado = true;
                } else if (esPasada && !headerPasadasMostrado) {
                    items.add("Tareas Pasadas");
                    headerPasadasMostrado = true;
                }
            }
            items.add(e);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            EventoLocal evento = (EventoLocal) items.get(position);
            ((TareaViewHolder) holder).bind(evento, longClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Object getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(android.R.id.text1);
            tv.setTextSize(14);
            tv.setPadding(32, 48, 0, 16);
            tv.setAllCaps(true);
            tv.setTextColor(Color.parseColor("#9CA3AF"));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        public void bind(String text) { tv.setText(text); }
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvEtiqueta, tvHora, tvTitulo, tvDesc;
        View viewColor;
        ImageView ivIcono, ivDelete;
        MaterialCardView cardIcono;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEtiqueta = itemView.findViewById(R.id.tvEtiquetaTipo);
            tvHora = itemView.findViewById(R.id.tvItemHora);
            tvTitulo = itemView.findViewById(R.id.tvItemTitulo);
            tvDesc = itemView.findViewById(R.id.tvItemDesc);
            viewColor = itemView.findViewById(R.id.viewColorIndicador);
            ivIcono = itemView.findViewById(R.id.ivTipoIcono);
            cardIcono = itemView.findViewById(R.id.ivIconoTarea);
            // Reutilizaremos el icono circular o añadiremos lógica para el borrado
        }

        public void bind(EventoLocal evento, OnTareaLongClickListener longClickListener) {
            tvTitulo.setText(evento.titulo);
            
            // Efecto visual de tachado si está completada
            if (evento.completado) {
                tvTitulo.setPaintFlags(tvTitulo.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitulo.setAlpha(0.5f);
                tvDesc.setAlpha(0.5f);
            } else {
                tvTitulo.setPaintFlags(tvTitulo.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitulo.setAlpha(1.0f);
                tvDesc.setAlpha(1.0f);
            }
            
            String desc = evento.descripcion;
            if (desc == null || desc.trim().isEmpty()) {
                desc = (evento.nombreGrupo != null) ? evento.nombreGrupo : "";
            }
            tvDesc.setText(desc);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMM", new Locale("es", "ES"));
            tvHora.setText(evento.fecha.format(formatter));

            String tipo = (evento.tipo != null) ? evento.tipo : "Evento";
            int colorPrimario = Color.parseColor("#818CF8");
            int colorFondoIcono = Color.parseColor("#22818CF8");
            int resIcono = android.R.drawable.ic_menu_edit;

            String textoEtiqueta = tipo.toUpperCase();
            LocalDate hoy = LocalDate.now();
            boolean esHoy = hoy.equals(evento.fecha);
            boolean esPasada = evento.fecha.isBefore(hoy);

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

            if (esPasada) {
                colorPrimario = Color.GRAY;
                colorFondoIcono = Color.parseColor("#11888888");
                textoEtiqueta = "PASADA";
                tvTitulo.setAlpha(0.6f);
                tvDesc.setAlpha(0.6f);
                cardIcono.setVisibility(View.GONE); // No permitir borrar si es pasada
            } else if (evento.completado) {
                cardIcono.setVisibility(View.GONE); // No permitir borrar si ya está completada
            } else {
                tvTitulo.setAlpha(1.0f);
                tvDesc.setAlpha(1.0f);
                cardIcono.setVisibility(View.VISIBLE);
            }

            tvEtiqueta.setText(textoEtiqueta);
            tvEtiqueta.setTextColor(colorPrimario);
            viewColor.setBackgroundColor(colorPrimario);
            cardIcono.setCardBackgroundColor(colorFondoIcono);
            ivIcono.setImageResource(resIcono);
            ivIcono.setImageTintList(ColorStateList.valueOf(colorPrimario));

            // Abrir detalle (en DetalleEventoActivity ya gestionaremos la edición)
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), DetalleEventoActivity.class);
                intent.putExtra("EVENTO_ID", evento.id);
                intent.putExtra("ES_PASADA", esPasada); // Pasar si es pasada
                itemView.getContext().startActivity(intent);
            });

            // El botón de la derecha ahora no es clicable por petición del usuario
            cardIcono.setOnClickListener(null);
            cardIcono.setClickable(false);
            cardIcono.setFocusable(false);

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onTareaLongClick(evento, itemView);
                }
                return true;
            });
        }
    }
}
