package com.example.agendaacademica.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.activities.AjustesActivity;
import com.example.agendaacademica.activities.FormularioActivity;
import com.example.agendaacademica.activities.ListaTareasActivity;
import com.example.agendaacademica.adapter.CalendarioAdapter;
import com.example.agendaacademica.adapter.TareaAdapter;
import com.example.agendaacademica.model.Evento;
import com.example.agendaacademica.model.Subtarea;
import com.example.agendaacademica.network.RetrofitClient;
import com.example.agendaacademica.utils.ClickGuard;
import com.example.agendaacademica.utils.GlideUtils;
import com.example.agendaacademica.viewmodel.DataSyncViewModel;
import com.example.agendaacademica.viewmodel.EventoViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento principal que representa la Agenda Académica.
 * Integra un calendario interactivo con una lista de tareas diaria.
 * Soporta gestos de deslizamiento (swipe) para completar o eliminar tareas de forma rápida,
 * sincronización en tiempo real con el servidor y gestión de caché offline.
 */
public class AgendaFragment extends Fragment implements CalendarioAdapter.OnItemListener, TareaAdapter.OnTareaLongClickListener {

    private TextView tvMesAnioCalendario, tvTituloPagina, tvSaludoAgenda;
    private ImageView ivPerfilAgenda;
    private RecyclerView recyclerViewCalendario, rvTareas;
    private TareaAdapter tareaAdapter;
    private LocalDate fechaSeleccionada;
    private SessionManager session;
    private SwipeRefreshLayout swipeRefresh;
    private DataSyncViewModel dataSyncViewModel;
    private View cardErrorBanner, cardAdminPanel;
    private EventoViewModel eventoViewModel;
    private final Gson gson = new Gson();

    private final List<EventoLocal> listaEventos = new ArrayList<>();
    private final List<EventoLocal> listaFiltradaTareas = new ArrayList<>();

    /**
     * Inicializa el fragmento y configura los ViewModels necesarios para la sincronización.
     * 
     * @param savedInstanceState Estado previo del fragmento.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());
        fechaSeleccionada = LocalDate.now();

        dataSyncViewModel = new ViewModelProvider(requireActivity()).get(DataSyncViewModel.class);
        eventoViewModel = new ViewModelProvider(this).get(EventoViewModel.class);
    }

    /**
     * Infla la vista del fragmento y configura los componentes de la interfaz.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agenda, container, false);
        initViews(view);
        configurarCalendario();

        dataSyncViewModel.getRefreshAgenda().observe(getViewLifecycleOwner(), refresh -> {
            if (refresh != null && refresh) cargarDatos();
        });

        cardAdminPanel = view.findViewById(R.id.cardAdminPanel);
        if (cardAdminPanel != null) {
            cardAdminPanel.setOnClickListener(v -> {
                if (ClickGuard.canClick()) {
                    startActivity(new Intent(getContext(), com.example.agendaacademica.activities.AdminActivity.class));
                }
            });
        }

        View btnRetry = view.findViewById(R.id.btnRetryBanner);
        if (btnRetry != null) btnRetry.setOnClickListener(v -> cargarDatos());

        return view;
    }

    /**
     * Vincula las vistas y configura los listeners de los botones.
     */
    private void initViews(View v) {
        tvSaludoAgenda = v.findViewById(R.id.tvSaludoAgenda);
        tvMesAnioCalendario = v.findViewById(R.id.tvMesAnioCalendario);
        tvTituloPagina = v.findViewById(R.id.tvTituloPagina);
        ivPerfilAgenda = v.findViewById(R.id.ivPerfilAgenda);
        
        recyclerViewCalendario = v.findViewById(R.id.recyclerViewCalendario);
        rvTareas = v.findViewById(R.id.rvTareas);
        swipeRefresh = v.findViewById(R.id.swipeRefreshAgenda);

        recyclerViewCalendario.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvTareas.setLayoutManager(new LinearLayoutManager(getContext()));
        
        tareaAdapter = new TareaAdapter(getContext(), listaFiltradaTareas, this);
        tareaAdapter.setMostrarHeaders(false); 
        rvTareas.setAdapter(tareaAdapter);
        configurarGestosRapidos();

        swipeRefresh.setOnRefreshListener(this::cargarDatos);

        v.findViewById(R.id.btnMesAnterior).setOnClickListener(view -> cambiarMes(-1));
        v.findViewById(R.id.btnMesSiguiente).setOnClickListener(view -> cambiarMes(1));
        v.findViewById(R.id.btn_settings).setOnClickListener(view -> {
            if (ClickGuard.canClick()) startActivity(new Intent(getContext(), AjustesActivity.class));
        });
        v.findViewById(R.id.fabAddEvento).setOnClickListener(view -> {
            if (ClickGuard.canClick()) irAFormulario();
        });
        v.findViewById(R.id.btnVerTodas).setOnClickListener(view -> {
            if (ClickGuard.canClick()) startActivity(new Intent(getContext(), ListaTareasActivity.class));
        });
    }

    /**
     * Cambia el mes visualizado en el calendario.
     * 
     * @param delta Cantidad de meses a sumar o restar.
     */
    private void cambiarMes(int delta) {
        fechaSeleccionada = fechaSeleccionada.plusMonths(delta);
        actualizarCalendarioYLista();
    }

    private void actualizarCalendarioYLista() {
        configurarCalendario();
        actualizarListaTareas();
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarDatosPerfil();
        cargarDatos();
    }

    /**
     * Actualiza el saludo y la foto de perfil en la cabecera.
     */
    private void actualizarDatosPerfil() {
        tvSaludoAgenda.setText(getString(R.string.hola_usuario, session.obtenerNombreUsuario()));
        if (ivPerfilAgenda != null) {
            GlideUtils.cargarFotoPerfil(requireContext(), session.obtenerFotoUsuario(), ivPerfilAgenda);
        }
        
        // El panel de administración solo es visible para usuarios con rol ADMIN.
        if (cardAdminPanel != null) {
            cardAdminPanel.setVisibility(session.esAdmin() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Recupera la lista de eventos del usuario desde el servidor.
     */
    private void cargarDatos() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        Long usuarioId = session.getUserId();

        RetrofitClient.getApiService().obtenerEventosUsuario(usuarioId).enqueue(new Callback<List<Evento>>() {
            @Override
            public void onResponse(@NonNull Call<List<Evento>> call, @NonNull Response<List<Evento>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    ocultarError();
                    procesarEventos(response.body());
                    session.guardarEventosCache(gson.toJson(response.body()));
                    if (eventoViewModel != null) eventoViewModel.refresh(usuarioId);
                } else {
                    cargarCacheYMostrarError(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Evento>> call, @NonNull Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                cargarCacheYMostrarError(true);
            }
        });
    }

    /**
     * Carga los datos de la caché si no hay conexión a internet.
     */
    private void cargarCacheYMostrarError(boolean porFalloRed) {
        if (porFalloRed) mostrarError();
        String cache = session.obtenerEventosCache();
        if (cache != null) {
            try {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Evento>>(){}.getType();
                List<Evento> eventos = gson.fromJson(cache, listType);
                procesarEventos(eventos);
            } catch (Exception e) {
                if (!porFalloRed) mostrarError();
            }
        } else if (!porFalloRed) {
            mostrarError();
        }
    }

    private void mostrarError() {
        if (cardErrorBanner != null) cardErrorBanner.setVisibility(View.VISIBLE);
    }

    private void ocultarError() {
        if (cardErrorBanner != null) cardErrorBanner.setVisibility(View.GONE);
    }

    /**
     * Procesa la lista de eventos recibida y la convierte al formato local para visualización.
     */
    private void procesarEventos(List<Evento> eventos) {
        listaEventos.clear();
        if (eventos == null) return;

        for (Evento e : eventos) {
            try {
                String rawFecha = e.getFecha();
                String fechaLimpia = "2000-01-01"; 
                if (rawFecha != null && rawFecha.length() >= 10) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(rawFecha);
                    if (m.find()) fechaLimpia = m.group(1);
                }

                LocalDate fecha = LocalDate.parse(fechaLimpia);
                EventoLocal el = new EventoLocal(e.getId(), fecha, e.getTitulo(), e.getHora(), e.getDescripcion(), e.getTipo());
                el.completado = e.isCompletado();
                el.setSubtareas(e.getSubtareas());
                el.setNombreGrupo(e.getGrupo() != null ? e.getGrupo().getNombre() : "SOLO_PARA_MI");
                
                listaEventos.add(el);
            } catch (Exception ignored) {}
        }
        actualizarCalendarioYLista();
    }

    /**
     * Configura el adaptador del calendario y marca los días que contienen tareas.
     */
    private void configurarCalendario() {
        ArrayList<String> dias = diasEnMesArray(fechaSeleccionada);
        Map<String, String> colores = new HashMap<>();

        for (EventoLocal e : listaEventos) {
            if (e.fecha.getMonthValue() == fechaSeleccionada.getMonthValue() && e.fecha.getYear() == fechaSeleccionada.getYear()) {
                colores.put(String.valueOf(e.fecha.getDayOfMonth()), getHexColor(e.tipo));
            }
        }

        recyclerViewCalendario.setAdapter(new CalendarioAdapter(dias, colores, fechaSeleccionada.getDayOfMonth(), 
                fechaSeleccionada.getMonthValue(), fechaSeleccionada.getYear(), this));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
        String mes = fechaSeleccionada.format(formatter);
        tvMesAnioCalendario.setText(mes.substring(0, 1).toUpperCase() + mes.substring(1));
    }

    private String getHexColor(String tipo) {
        if (tipo == null) return "#818CF8";
        switch (tipo) {
            case "Deberes": return "#3B82F6";
            case "Proyecto": return "#F59E0B";
            case "Examen": return "#EF4444";
            case "Estudio": return "#10B981";
            default: return "#818CF8";
        }
    }

    private ArrayList<String> diasEnMesArray(LocalDate date) {
        ArrayList<String> dias = new ArrayList<>();
        YearMonth ym = YearMonth.from(date);
        int offset = date.withDayOfMonth(1).getDayOfWeek().getValue() - 1;
        int slots = (int) Math.ceil((ym.lengthOfMonth() + offset) / 7.0) * 7;
        for (int i = 1; i <= slots; i++) {
            if (i <= offset || i > offset + ym.lengthOfMonth()) dias.add("");
            else dias.add(String.valueOf(i - offset));
        }
        return dias;
    }

    @Override
    public void onItemClick(int position, String diaText) {
        if (diaText != null && !diaText.isEmpty()) {
            fechaSeleccionada = fechaSeleccionada.withDayOfMonth(Integer.parseInt(diaText));
            actualizarCalendarioYLista();
        }
    }

    /**
     * Filtra la lista de tareas para mostrar únicamente las correspondientes al día seleccionado.
     */
    private void actualizarListaTareas() {
        if (rvTareas == null) return;

        if (tvTituloPagina != null) {
            if (fechaSeleccionada.equals(LocalDate.now())) {
                tvTituloPagina.setText(R.string.tareas_hoy);
            } else {
                String fechaFormateada = fechaSeleccionada.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()));
                tvTituloPagina.setText(getString(R.string.tareas_del_dia, fechaFormateada));
            }
        }

        listaFiltradaTareas.clear();
        for (EventoLocal e : listaEventos) {
            if (e.fecha.equals(fechaSeleccionada)) listaFiltradaTareas.add(e);
        }
        tareaAdapter.setTareas(listaFiltradaTareas);
    }

    @Override
    public void onTareaLongClick(EventoLocal evento, View view) {}

    /**
     * Configura el ItemTouchHelper para soportar gestos de deslizamiento en las tareas.
     */
    private void configurarGestosRapidos() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder instanceof TareaAdapter.HeaderViewHolder) return 0;
                int pos = viewHolder.getAdapterPosition();
                Object item = tareaAdapter.getItemAt(pos);
                if (item instanceof EventoLocal && ((EventoLocal) item).completado) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                EventoLocal evento = (EventoLocal) tareaAdapter.getItemAt(pos);
                if (evento == null) return;

                if (direction == ItemTouchHelper.RIGHT) {
                    if (evento.completado) {
                        tareaAdapter.notifyItemChanged(pos);
                    } else {
                        marcarComoCompletado(evento, pos);
                    }
                } else if (direction == ItemTouchHelper.LEFT) {
                    prepararBorradoConDeshacer(evento, pos);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int state, boolean active) {
                if (state == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    dibujarFondoSwipe(c, vh.itemView, dX);
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rvTareas);
    }

    /**
     * Dibuja los fondos de colores y los iconos durante el gesto de deslizamiento.
     */
    private void dibujarFondoSwipe(Canvas c, View itemView, float dX) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        float density = itemView.getContext().getResources().getDisplayMetrics().density;
        float cornerRadius = 28 * density; 
        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());

        if (dX > 0) { // Swipe derecha: marcar como completada
            p.setColor(Color.parseColor("#22C55E"));
            c.drawRoundRect(background, cornerRadius, cornerRadius, p);
            dibujarIcono(c, itemView, dX, R.drawable.ic_check, true);
        } else if (dX < 0) { // Swipe izquierda: eliminar tarea
            p.setColor(Color.parseColor("#EF4444"));
            c.drawRoundRect(background, cornerRadius, cornerRadius, p);
            dibujarIcono(c, itemView, dX, android.R.drawable.ic_menu_delete, false);
        }
    }

    private void dibujarIcono(Canvas c, View itemView, float dX, int iconRes, boolean isRight) {
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon == null) return;
        float density = itemView.getContext().getResources().getDisplayMetrics().density;
        float height = (float) itemView.getBottom() - (float) itemView.getTop();
        float alpha = Math.min(1.0f, Math.abs(dX) / 150f);
        int iconSize = (int) (height * 0.4f);
        int iconTop = itemView.getTop() + (int) (height - iconSize) / 2;
        int margin = (int)(32 * density);

        if (isRight) {
            int iconLeft = itemView.getLeft() + margin;
            icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        } else {
            int iconRight = itemView.getRight() - margin;
            icon.setBounds(iconRight - iconSize, iconTop, iconRight, iconTop + iconSize);
        }
        icon.setTint(Color.WHITE);
        icon.setAlpha((int)(alpha * 255));
        icon.draw(c);
    }

    /**
     * Envía la solicitud al servidor para marcar una tarea como completada.
     */
    private void marcarComoCompletado(EventoLocal evento, int position) {
        RetrofitClient.getApiService().marcarEventoCompletado(evento.id, session.getUserId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) cargarDatos();
                else tareaAdapter.notifyItemChanged(position);
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                tareaAdapter.notifyItemChanged(position);
            }
        });
    }

    /**
     * Elimina visualmente la tarea y muestra un Snackbar con la opción de deshacer la acción.
     */
    private void prepararBorradoConDeshacer(EventoLocal evento, int position) {
        listaFiltradaTareas.remove(evento);
        listaEventos.remove(evento);
        tareaAdapter.setTareas(listaFiltradaTareas);

        Snackbar snackbar = Snackbar.make(rvTareas, R.string.tarea_eliminada, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.deshacer, v -> {
            if (!listaFiltradaTareas.contains(evento)) {
                listaEventos.add(evento);
                listaFiltradaTareas.add(position, evento);
                tareaAdapter.setTareas(listaFiltradaTareas);
            }
        });

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) borrarEventoDefinitivo(evento.id);
            }
        });
        snackbar.show();
    }

    private void borrarEventoDefinitivo(Long id) {
        RetrofitClient.getApiService().eliminarEvento(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) cargarDatos();
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    @Override
    public void onTareaDeleteClick(EventoLocal evento) {
        new android.app.AlertDialog.Builder(getContext())
            .setTitle(R.string.eliminar_evento_titulo)
            .setMessage(R.string.eliminar_evento_msg)
            .setPositiveButton(R.string.eliminar, (d, w) -> borrarEventoDefinitivo(evento.id))
            .setNegativeButton(R.string.cancelar, null)
            .show();
    }

    private void irAFormulario() {
        Intent intent = new Intent(getContext(), FormularioActivity.class);
        intent.putExtra("FECHA_SELECCIONADA", fechaSeleccionada.toString());
        startActivity(intent);
    }

    /**
     * Clase de datos que representa un evento simplificado para la lógica interna del fragmento.
     */
    public static class EventoLocal {
        public Long id; public LocalDate fecha; public String titulo; public String hora;
        public String descripcion; public String tipo; public String nombreGrupo;
        public boolean completado;
        private List<Subtarea> subtareas;
        public EventoLocal(Long id, LocalDate fecha, String titulo, String hora, String descripcion, String tipo) {
            this.id = id; this.fecha = fecha; this.titulo = titulo; this.hora = hora;
            this.descripcion = descripcion; this.tipo = tipo != null ? tipo : "Evento";
        }
        public List<Subtarea> getSubtareas() { return subtareas; }
        public void setSubtareas(List<Subtarea> s) { this.subtareas = s; }
        public void setNombreGrupo(String n) { this.nombreGrupo = n; }
    }
}
