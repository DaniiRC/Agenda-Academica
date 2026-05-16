package com.example.edusync.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.example.edusync.adapter.TareaAdapter;
import com.example.edusync.fragments.EduSyncFragment.EventoLocal;
import com.example.edusync.model.Asignatura;
import com.example.edusync.model.Evento;
import com.example.edusync.model.Grupo;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad que presenta una lista exhaustiva de todas las tareas del usuario.
 * Proporciona potentes herramientas de filtrado por clase, asignatura, tipo de tarea
 * y búsqueda por texto, además de permitir el borrado masivo de elementos filtrados.
 */
public class ListaTareasActivity extends BaseActivity implements TareaAdapter.OnTareaLongClickListener {

    private RecyclerView rvTodasTareas;
    private TareaAdapter adapter;
    private List<Evento> listaOriginal = new ArrayList<>();
    private List<Evento> listaFiltrada = new ArrayList<>();
    private ApiService apiService;
    private SessionManager session;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    
    private Grupo grupoFiltro;
    private Asignatura asignaturaFiltro;
    private String tipoFiltro = "Todo"; // Mantenemos "Todo" como valor interno de lógica
    private MaterialCardView cardFilterSummary;
    private TextView tvFilterText;
    private ImageView btnDeleteAll;
    private String queryFiltro = "";
    private View cardErrorBanner;
    private com.google.android.material.button.MaterialButton btnRetryBanner;
    private com.google.gson.Gson gson = new com.google.gson.Gson();

    /**
     * Inicializa la actividad y comienza la carga de tareas.
     * 
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_tareas);

        apiService = RetrofitClient.getApiService();
        session = new SessionManager(this);

        initViews();
        cargarTareas();
    }

    /**
     * Vincula las vistas y configura los gestores de eventos para filtros y búsqueda.
     */
    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilter).setOnClickListener(v -> mostrarDialogoFiltros());
        findViewById(R.id.btnClearFilters).setOnClickListener(v -> resetFiltros());

        rvTodasTareas = findViewById(R.id.rvTodasTareas);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        cardFilterSummary = findViewById(R.id.cardFilterSummary);
        tvFilterText = findViewById(R.id.tvFilterText);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);

        rvTodasTareas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TareaAdapter(this, new ArrayList<>(), this);
        rvTodasTareas.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::cargarTareas);
        btnDeleteAll.setOnClickListener(v -> mostrarConfirmacionBorradoMasivo());

        configurarBusqueda();
        
        cardErrorBanner = findViewById(R.id.cardErrorBanner);
        btnRetryBanner = findViewById(R.id.btnRetryBanner);
        if (btnRetryBanner != null) {
            btnRetryBanner.setOnClickListener(v -> cargarTareas());
        }
    }

    /**
     * Configura el componente de búsqueda por texto.
     */
    private void configurarBusqueda() {
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchViewTareas);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                queryFiltro = query;
                aplicarFiltros();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                queryFiltro = newText;
                aplicarFiltros();
                return true;
            }
        });
    }

    /**
     * Solicita la lista completa de eventos del usuario al servidor.
     * Implementa un sistema de caché para permitir la visualización en caso de fallo de red.
     */
    private void cargarTareas() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        ocultarError();
        
        long userId = session.getUserId();
        apiService.obtenerEventosUsuario(userId).enqueue(new Callback<List<Evento>>() {
            @Override
            public void onResponse(@NonNull Call<List<Evento>> call, @NonNull Response<List<Evento>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    listaOriginal = response.body();
                    session.guardarEventosCache(gson.toJson(listaOriginal));
                    aplicarFiltros();
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
     * Intenta recuperar los datos desde la caché local cuando la red no está disponible.
     * 
     * @param porFalloRed Verdadero si la llamada fue provocada por una excepción de red.
     */
    private void cargarCacheYMostrarError(boolean porFalloRed) {
        if (porFalloRed) mostrarError();

        String cache = session.obtenerEventosCache();
        if (cache != null) {
            try {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Evento>>(){}.getType();
                listaOriginal = gson.fromJson(cache, listType);
                aplicarFiltros();
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
     * Despliega un diálogo de tipo BottomSheet para la selección de criterios de filtrado.
     */
    private void mostrarDialogoFiltros() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filtros_tareas, null);
        
        TextView tvClass = view.findViewById(R.id.tvFilterClass);
        TextView tvSubject = view.findViewById(R.id.tvFilterSubject);
        ChipGroup cgType = view.findViewById(R.id.cgFilterType);
        
        if (grupoFiltro != null) tvClass.setText(grupoFiltro.getNombre());
        if (asignaturaFiltro != null) tvSubject.setText(asignaturaFiltro.getNombre());
        
        configurarChipsFiltro(cgType);

        view.findViewById(R.id.cardFilterClass).setOnClickListener(v -> abrirSelectorClase(tvClass, tvSubject));
        view.findViewById(R.id.cardFilterSubject).setOnClickListener(v -> abrirSelectorAsignatura(tvSubject));

        view.findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            int checkedId = cgType.getCheckedChipId();
            if (checkedId == R.id.chipTypeTask) tipoFiltro = "Tareas";
            else if (checkedId == R.id.chipTypeProject) tipoFiltro = "Proyecto";
            else if (checkedId == R.id.chipTypeStudy) tipoFiltro = "Estudio";
            else tipoFiltro = "Todo";
            
            aplicarFiltros();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void configurarChipsFiltro(ChipGroup cgType) {
        if ("Tareas".equals(tipoFiltro)) cgType.check(R.id.chipTypeTask);
        else if ("Proyecto".equals(tipoFiltro)) cgType.check(R.id.chipTypeProject);
        else if ("Estudio".equals(tipoFiltro)) cgType.check(R.id.chipTypeStudy);
        else cgType.check(R.id.chipTypeAll);
    }

    private void abrirSelectorClase(TextView tvClass, TextView tvSubject) {
        apiService.obtenerGruposDeUsuario(session.getUserId()).enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(Call<List<Grupo>> call, Response<List<Grupo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Grupo> grupos = response.body();
                    String[] nombres = new String[grupos.size() + 1];
                    nombres[0] = getString(R.string.todas_las_clases);
                    for (int i = 0; i < grupos.size(); i++) nombres[i+1] = grupos.get(i).getNombre();
                    
                    new AlertDialog.Builder(ListaTareasActivity.this)
                        .setItems(nombres, (d, which) -> {
                            if (which == 0) grupoFiltro = null;
                            else grupoFiltro = grupos.get(which-1);
                            tvClass.setText(which == 0 ? getString(R.string.todas_las_clases) : grupoFiltro.getNombre());
                            asignaturaFiltro = null;
                            tvSubject.setText(getString(R.string.todas_las_asignaturas));
                        }).show();
                }
            }
            @Override public void onFailure(Call<List<Grupo>> call, Throwable t) {}
        });
    }

    private void abrirSelectorAsignatura(TextView tvSubject) {
        if (grupoFiltro == null) return;
        apiService.obtenerAsignaturasDeGrupo(grupoFiltro.getId()).enqueue(new Callback<List<Asignatura>>() {
            @Override
            public void onResponse(Call<List<Asignatura>> call, Response<List<Asignatura>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Asignatura> asigs = response.body();
                    String[] nombres = new String[asigs.size() + 1];
                    nombres[0] = getString(R.string.todas_las_asignaturas);
                    for (int i = 0; i < asigs.size(); i++) nombres[i+1] = asigs.get(i).getNombre();

                    new AlertDialog.Builder(ListaTareasActivity.this)
                        .setItems(nombres, (d, which) -> {
                            if (which == 0) asignaturaFiltro = null;
                            else asignaturaFiltro = asigs.get(which-1);
                            tvSubject.setText(which == 0 ? getString(R.string.todas_las_asignaturas) : asignaturaFiltro.getNombre());
                        }).show();
                }
            }
            @Override public void onFailure(Call<List<Asignatura>> call, Throwable t) {}
        });
    }

    /**
     * Aplica los criterios de filtrado y búsqueda sobre la lista original y actualiza el adaptador.
     * Realiza una ordenación inteligente: primero las tareas futuras/hoy (orden cronológico),
     * seguidas de las tareas pasadas (orden cronológico inverso).
     */
    private void aplicarFiltros() {
        listaFiltrada = listaOriginal.stream().filter(e -> {
            boolean passGrupo = (grupoFiltro == null) || (e.getGrupo() != null && e.getGrupo().getId().equals(grupoFiltro.getId()));
            boolean passAsig = (asignaturaFiltro == null) || (e.getAsignatura() != null && e.getAsignatura().getId().equals(asignaturaFiltro.getId()));
            boolean passTipo = "Todo".equals(tipoFiltro) || tipoFiltro.equals(e.getTipo());
            
            boolean passQuery = true;
            if (queryFiltro != null && !queryFiltro.isEmpty()) {
                String q = queryFiltro.toLowerCase();
                boolean tituloMatch = e.getTitulo() != null && e.getTitulo().toLowerCase().contains(q);
                boolean asigMatch = e.getAsignatura() != null && e.getAsignatura().getNombre().toLowerCase().contains(q);
                boolean descMatch = e.getDescripcion() != null && e.getDescripcion().toLowerCase().contains(q);
                passQuery = tituloMatch || asigMatch || descMatch;
            }
            
            return passGrupo && passAsig && passTipo && passQuery;
        }).collect(Collectors.toList());

        List<EventoLocal> listaLocales = new ArrayList<>();
        for (Evento e : listaFiltrada) {
            try {
                LocalDate fecha = LocalDate.parse(e.getFecha());
                EventoLocal el = new EventoLocal(e.getId(), fecha, e.getTitulo(), e.getHora(), e.getDescripcion(), e.getTipo());
                el.setNombreGrupo(e.getGrupo() != null ? e.getGrupo().getNombre() : "General");
                el.setSubtareas(e.getSubtareas());
                el.completado = e.isCompletado();
                listaLocales.add(el);
            } catch (Exception ignored) {}
        }

        LocalDate hoy = LocalDate.now();
        List<EventoLocal> actuales = listaLocales.stream()
                .filter(e -> !e.fecha.isBefore(hoy))
                .sorted(Comparator.comparing(e -> e.fecha))
                .collect(Collectors.toList());

        List<EventoLocal> pasadas = listaLocales.stream()
                .filter(e -> e.fecha.isBefore(hoy))
                .sorted((e1, e2) -> e2.fecha.compareTo(e1.fecha))
                .collect(Collectors.toList());

        List<EventoLocal> listaOrdenada = new ArrayList<>();
        listaOrdenada.addAll(actuales);
        listaOrdenada.addAll(pasadas);

        adapter.setTareas(listaOrdenada);
        actualizarCardFiltros();
    }

    @Override
    public void onTareaLongClick(EventoLocal evento, View view) {}

    @Override
    public void onTareaDeleteClick(EventoLocal evento) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.eliminar_tarea)
            .setMessage(R.string.confirmar_eliminar_tarea)
            .setPositiveButton(R.string.eliminar, (dialog, which) -> borrarTarea(evento.id))
            .setNegativeButton(R.string.cancelar, null)
            .show();
    }

    /**
     * Elimina una tarea individual del servidor.
     * 
     * @param id ID de la tarea a eliminar.
     */
    private void borrarTarea(Long id) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.eliminarEvento(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(ListaTareasActivity.this, R.string.tarea_eliminada, Toast.LENGTH_SHORT).show();
                    cargarTareas();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ListaTareasActivity.this, R.string.error_eliminar, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Muestra una confirmación antes de proceder con el borrado de múltiples tareas.
     */
    private void mostrarConfirmacionBorradoMasivo() {
        if (listaFiltrada.isEmpty()) return;
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.borrado_masivo)
            .setMessage(getString(R.string.borrar_todas_filtros, listaFiltrada.size()))
            .setPositiveButton(R.string.eliminar_todas, (dialog, which) -> borrarTareasMasivo())
            .setNegativeButton(R.string.cancelar, null)
            .show();
    }

    /**
     * Ejecuta el borrado secuencial de todas las tareas presentes en el filtro actual.
     */
    private void borrarTareasMasivo() {
        progressBar.setVisibility(View.VISIBLE);
        for (Evento e : listaFiltrada) {
            apiService.eliminarEvento(e.getId()).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override public void onFailure(Call<Void> call, Throwable t) {}
            });
        }
        rvTodasTareas.postDelayed(this::cargarTareas, 1000);
    }

    /**
     * Actualiza la visibilidad y el contenido del resumen de filtros activos.
     */
    private void actualizarCardFiltros() {
        if (grupoFiltro == null && asignaturaFiltro == null && "Todo".equals(tipoFiltro)) {
            cardFilterSummary.setVisibility(View.GONE);
            btnDeleteAll.setVisibility(View.GONE);
        } else {
            cardFilterSummary.setVisibility(View.VISIBLE);
            btnDeleteAll.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder(getString(R.string.filtros_con_dos_puntos));
            if (grupoFiltro != null) sb.append(grupoFiltro.getNombre()).append(" ");
            if (asignaturaFiltro != null) sb.append("| ").append(asignaturaFiltro.getNombre()).append(" ");
            if (!"Todo".equals(tipoFiltro)) sb.append("| ").append(tipoFiltro);
            tvFilterText.setText(sb.toString());
        }
    }

    /**
     * Restablece todos los filtros de búsqueda a su estado inicial.
     */
    private void resetFiltros() {
        grupoFiltro = null;
        asignaturaFiltro = null;
        tipoFiltro = "Todo";
        aplicarFiltros();
    }
}
