package com.example.agendaacademica.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import androidx.core.graphics.ColorUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agendaacademica.R;
import com.example.agendaacademica.activities.PerfilClaseActivity;
import com.example.agendaacademica.model.Grupo;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GrupoAdapter extends RecyclerView.Adapter<GrupoAdapter.ViewHolder> {

    private List<Grupo> listaGrupos;
    private final List<Grupo> listaGruposFull;
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    public GrupoAdapter(List<Grupo> listaGrupos) {
        this.listaGrupos = new ArrayList<>(listaGrupos);
        this.listaGruposFull = new ArrayList<>(listaGrupos);
    }

    public void updateList(List<Grupo> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GrupoDiffCallback(this.listaGrupos, newList));
        this.listaGrupos.clear();
        this.listaGrupos.addAll(newList);
        this.listaGruposFull.clear();
        this.listaGruposFull.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void filter(String text) {
        List<Grupo> filteredList = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            filteredList.addAll(listaGruposFull);
        } else {
            String filterPattern = text.toLowerCase().trim();
            for (Grupo grupo : listaGruposFull) {
                if (grupo.getNombre().toLowerCase().contains(filterPattern) ||
                    (grupo.getCodigo() != null && grupo.getCodigo().toLowerCase().contains(filterPattern))) {
                    filteredList.add(grupo);
                }
            }
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GrupoDiffCallback(this.listaGrupos, filteredList));
        this.listaGrupos.clear();
        this.listaGrupos.addAll(filteredList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clase, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(listaGrupos.get(position));
    }

    @Override
    public int getItemCount() {
        return listaGrupos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre, tvInfoSecundaria, tvCountParticipantes, tvAdminTag;
        private final ShapeableImageView ivFoto;
        private final View viewColorLateral, viewDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreAsignatura);
            tvInfoSecundaria = itemView.findViewById(R.id.tvInfoSecundaria);
            tvCountParticipantes = itemView.findViewById(R.id.tvCountParticipantes);
            ivFoto = itemView.findViewById(R.id.ivClaseImagen);
            viewColorLateral = itemView.findViewById(R.id.viewColorLateral);
            tvAdminTag = itemView.findViewById(R.id.tvAdminTag);
            viewDot = itemView.findViewById(R.id.dotSeparator);
        }

        public void bind(final Grupo grupo) {
            tvNombre.setText(grupo.getNombre());
            
            // Color lateral dinámico con gradiente
            String colorHex = grupo.getColor();
            if (colorHex != null && colorHex.startsWith("#")) {
                try {
                    int color = Color.parseColor(colorHex);
                    // Creamos un gradiente que va del color seleccionado a una versión más clara/transparente
                    GradientDrawable gradient = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{color, ColorUtils.setAlphaComponent(color, 40)}
                    );
                    gradient.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, itemView.getResources().getDisplayMetrics()));
                    viewColorLateral.setBackground(gradient);
                } catch (Exception e) {
                    viewColorLateral.setBackgroundResource(R.drawable.gradient_blue_line);
                }
            } else {
                // Fallback si no hay color o falla el parseo
                viewColorLateral.setBackgroundResource(R.drawable.gradient_blue_line);
            }

            String info = (grupo.getCodigo() != null && !grupo.getCodigo().isEmpty()) 
                ? "Código: " + grupo.getCodigo() 
                : "";
            tvInfoSecundaria.setText(info);
            tvInfoSecundaria.setVisibility(info.isEmpty() ? View.GONE : View.VISIBLE);

            int numMiembros = grupo.getAlumnos() != null ? grupo.getAlumnos().size() : 0;
            tvCountParticipantes.setText(String.format(Locale.getDefault(), "%d miembros", numMiembros));

            // Gestión de la etiqueta ADMIN
            com.example.agendaacademica.SessionManager session = new com.example.agendaacademica.SessionManager(itemView.getContext());
            boolean isAdmin = grupo.getProfesor() != null && grupo.getProfesor().getId().equals(session.getUserId());
            if (isAdmin) {
                tvAdminTag.setVisibility(View.VISIBLE);
                if (viewDot != null) viewDot.setVisibility(View.VISIBLE);
            } else {
                tvAdminTag.setVisibility(View.GONE);
                if (viewDot != null) viewDot.setVisibility(View.GONE);
            }

            String fotoUrl = grupo.getFotoUrl();
            android.util.Log.d("GrupoAdapter", "Cargando foto para el grupo '" + grupo.getNombre() + "'. URL recibida del backend: " + fotoUrl);
            com.example.agendaacademica.utils.GlideUtils.cargarFotoGrupo(itemView.getContext(), fotoUrl, ivFoto);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), PerfilClaseActivity.class);
                intent.putExtra("CLASE_ID", grupo.getId());
                intent.putExtra("CLASE_NOMBRE", grupo.getNombre());
                intent.putExtra("CLASE_DESC", grupo.getDescripcion());
                intent.putExtra("CLASE_FOTO", fotoUrl);
                intent.putExtra("CLASE_CODIGO", grupo.getCodigo());
                intent.putExtra("PROFESOR_ID", grupo.getProfesor() != null ? grupo.getProfesor().getId() : -1L);
                v.getContext().startActivity(intent);
            });
        }
    }

    private static class GrupoDiffCallback extends DiffUtil.Callback {
        private final List<Grupo> oldList;
        private final List<Grupo> newList;

        public GrupoDiffCallback(List<Grupo> oldList, List<Grupo> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId().equals(newList.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Grupo oldG = oldList.get(oldPos);
            Grupo newG = newList.get(newPos);
            
            boolean sameName = (oldG.getNombre() != null) ? oldG.getNombre().equals(newG.getNombre()) : newG.getNombre() == null;
            boolean sameCode = (oldG.getCodigo() != null) ? oldG.getCodigo().equals(newG.getCodigo()) : newG.getCodigo() == null;
            boolean sameFoto = (oldG.getFotoUrl() != null) ? oldG.getFotoUrl().equals(newG.getFotoUrl()) : newG.getFotoUrl() == null;
            boolean sameColor = (oldG.getColor() != null) ? oldG.getColor().equals(newG.getColor()) : newG.getColor() == null;
            
            return sameName && sameCode && sameFoto && sameColor &&
                   (oldG.getAlumnos() != null && newG.getAlumnos() != null && oldG.getAlumnos().size() == newG.getAlumnos().size());
        }
    }
}
