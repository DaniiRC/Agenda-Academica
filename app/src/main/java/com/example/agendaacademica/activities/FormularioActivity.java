package com.example.agendaacademica.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.model.Asignatura;
import com.example.agendaacademica.model.Evento;
import com.example.agendaacademica.model.Grupo;
import com.example.agendaacademica.model.Subtarea;
import com.example.agendaacademica.model.Usuario;
import com.example.agendaacademica.network.ApiService;
import com.example.agendaacademica.network.RetrofitClient;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad para la creación y edición de eventos académicos.
 * Proporciona una interfaz completa para definir títulos, descripciones, fechas,
 * horarios, categorías (Deberes, Proyecto, Examen), subtareas y visibilidad (Privada o Grupo).
 */
public class FormularioActivity extends BaseActivity {

    private ApiService apiService;
    private SessionManager session;

    private EditText etEventTitle, etEventNotes;
    private TextView tvStartDate, tvStartTime, tvEndDate, tvEndTime, tvReminderValue, btnSave;
    private LinearLayout llTaskList, llDndOptions;
    private SwitchCompat switchPomodoro;
    private MaterialCardView chipDeberes, chipProyecto, chipExamen;
    
    private String categoriaSeleccionada = "Deberes";
    private final List<Subtarea> listaSubtareas = new ArrayList<>();
    private Grupo grupoSeleccionado;
    private Asignatura asignaturaSeleccionada;
    private List<Grupo> cachedGrupos = new ArrayList<>();
    private Calendar calendarInicio, calendarFin;
    private Long eventoEdicionId = -1L;
    private Evento eventoOriginal;
    private List<Asignatura> cachedAsignaturas = new ArrayList<>();
    private Long lastGrupoIdForAsignaturas = -1L;

    /**
     * Inicializa la actividad y configura los servicios necesarios.
     * 
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        apiService = RetrofitClient.getApiService();
        session = new SessionManager(this);

        initViews();
        configurarUX();
    }

    /**
     * Inicializa las referencias a las vistas y configura los calendarios por defecto.
     */
    private void initViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etEventNotes = findViewById(R.id.etEventNotes);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvEndTime = findViewById(R.id.tvEndTime);
        tvReminderValue = findViewById(R.id.tvReminderValue);
        btnSave = findViewById(R.id.btnSave);
        llTaskList = findViewById(R.id.llTaskList);
        switchPomodoro = findViewById(R.id.switchPomodoro);
        llDndOptions = findViewById(R.id.llDndOptions);
        chipDeberes = findViewById(R.id.chipDeberes);
        chipProyecto = findViewById(R.id.chipProyecto);
        chipExamen = findViewById(R.id.chipExamen);

        calendarInicio = Calendar.getInstance();
        calendarFin = Calendar.getInstance();
        
        // Manejo de fecha preseleccionada desde la Agenda
        String fechaIntent = getIntent().getStringExtra("FECHA_SELECCIONADA");
        if (fechaIntent != null) {
            try {
                java.time.LocalDate ld = java.time.LocalDate.parse(fechaIntent);
                calendarInicio.set(ld.getYear(), ld.getMonthValue() - 1, ld.getDayOfMonth());
                calendarFin.set(ld.getYear(), ld.getMonthValue() - 1, ld.getDayOfMonth());
            } catch (Exception ignored) {}
        }
        
        calendarFin.add(Calendar.HOUR_OF_DAY, 1);
        
        eventoEdicionId = getIntent().getLongExtra("EVENTO_ID", -1L);
        if (eventoEdicionId != -1L) {
            cargarDatosEventoEdicion();
        } else {
            long grupoIdIntent = getIntent().getLongExtra("GRUPO_ID", -1L);
            if (grupoIdIntent != -1L) {
                grupoSeleccionado = new Grupo();
                grupoSeleccionado.setId(grupoIdIntent);
                grupoSeleccionado.setNombre(getIntent().getStringExtra("GRUPO_NOMBRE"));
                actualizarUIFiltros();
            }
        }
        
        if (llDndOptions != null) llDndOptions.setVisibility(View.GONE);
        actualizarTextosFechaHora();
    }

    /**
     * Configura la interacción del usuario con los elementos de la interfaz.
     */
    private void configurarUX() {
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> intentarCerrar());
        btnSave.setOnClickListener(v -> guardarEvento());
        findViewById(R.id.btnAddStep).setOnClickListener(v -> añadirSubtareaVacia());

        tvStartDate.setOnClickListener(v -> mostrarDatePicker(calendarInicio, true));
        tvStartTime.setOnClickListener(v -> mostrarTimePicker(calendarInicio, true));
        tvEndDate.setOnClickListener(v -> mostrarDatePicker(calendarFin, false));
        tvEndTime.setOnClickListener(v -> mostrarTimePicker(calendarFin, false));
        
        configurarRecordatorios();
        configurarModoConcentracion();
        configurarChips();
        configurarVisibilidad();
    }

    /**
     * Configura el selector de recordatorios y su animación.
     */
    private void configurarRecordatorios() {
        View btnReminder = findViewById(R.id.btnReminder);
        if (btnReminder != null) {
            btnReminder.setOnClickListener(v -> {
                LinearLayout container = findViewById(R.id.llReminderListContainer);
                ImageView arrow = (ImageView) ((LinearLayout) v).getChildAt(3);
                if (container.getVisibility() == View.VISIBLE) {
                    container.setVisibility(View.GONE);
                    arrow.animate().rotation(0).setDuration(200).start();
                } else {
                    container.setVisibility(View.VISIBLE);
                    arrow.animate().rotation(180).setDuration(200).start();
                    poblarListaRecordatorios(container, arrow);
                }
            });
        }
    }

    /**
     * Configura el interruptor del modo concentración (Pomodoro) y DND.
     */
    private void configurarModoConcentracion() {
        if (switchPomodoro != null) {
            switchPomodoro.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (llDndOptions != null) llDndOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }
    }

    /**
     * Gestiona la lógica de visibilidad y selección de grupos/asignaturas.
     */
    private void configurarVisibilidad() {
        MaterialCardView btnPrivate = findViewById(R.id.btnVisibilityPrivate);
        View btnClassHeader = findViewById(R.id.btnVisibilityClass);
        View btnAsigHeader = findViewById(R.id.btnVisibilityAsignatura);

        if (btnPrivate != null) btnPrivate.setOnClickListener(v -> seleccionarPrivado());

        if (btnClassHeader != null) {
            btnClassHeader.setOnClickListener(v -> toggleList(findViewById(R.id.llClassListContainer), findViewById(R.id.ivArrowClass), true));
        }

        if (btnAsigHeader != null) {
            btnAsigHeader.setOnClickListener(v -> toggleList(findViewById(R.id.llAsignaturaListContainer), findViewById(R.id.ivArrowAsignatura), false));
        }

        apiService.obtenerGruposDeUsuario(session.getUserId()).enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Grupo>> call, @NonNull Response<List<Grupo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedGrupos = response.body();
                    if (!cachedGrupos.isEmpty()) {
                        findViewById(R.id.cardVisibilityClass).setVisibility(View.VISIBLE);
                    }
                    actualizarUIFiltros();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Grupo>> call, @NonNull Throwable t) {}
        });
    }

    /**
     * Alterna la visibilidad de los contenedores desplegables (clases o asignaturas).
     */
    private void toggleList(LinearLayout container, ImageView arrow, boolean isClass) {
        if (container == null || arrow == null) return;
        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(View.GONE);
            arrow.animate().rotation(0).setDuration(200).start();
        } else {
            container.setVisibility(View.VISIBLE);
            arrow.animate().rotation(180).setDuration(200).start();
            if (isClass) {
                poblarListaClases(container, cachedGrupos);
            } else if (grupoSeleccionado != null) {
                if (grupoSeleccionado.getId().equals(lastGrupoIdForAsignaturas) && !cachedAsignaturas.isEmpty()) {
                    desplegarAsignaturas(cachedAsignaturas);
                } else {
                    cargarAsignaturasYDesplegar(grupoSeleccionado);
                }
            }
        }
    }

    /**
     * Pobla dinámicamente la lista de clases disponibles para el usuario.
     */
    private void poblarListaClases(LinearLayout container, List<Grupo> grupos) {
        container.removeAllViews();
        for (Grupo g : grupos) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_selector_opcion, container, false);
            TextView tvNombre = itemView.findViewById(R.id.tvOpcionNombre);
            if (tvNombre != null) tvNombre.setText(g.getNombre());

            itemView.setOnClickListener(v -> {
                grupoSeleccionado = g;
                asignaturaSeleccionada = null;
                cachedAsignaturas.clear();
                lastGrupoIdForAsignaturas = -1L;
                actualizarUIFiltros();
                container.setVisibility(View.GONE);
                findViewById(R.id.ivArrowClass).animate().rotation(0).setDuration(200).start();
                cargarAsignaturasYDesplegar(g);
            });
            container.addView(itemView);
        }
    }

    /**
     * Obtiene y muestra las asignaturas asociadas al grupo seleccionado.
     * 
     * @param grupo Grupo del cual obtener las asignaturas.
     */
    private void cargarAsignaturasYDesplegar(Grupo grupo) {
        apiService.obtenerAsignaturasDeGrupo(grupo.getId()).enqueue(new Callback<List<Asignatura>>() {
            @Override
            public void onResponse(Call<List<Asignatura>> call, Response<List<Asignatura>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedAsignaturas = response.body();
                    lastGrupoIdForAsignaturas = grupo.getId();
                    desplegarAsignaturas(cachedAsignaturas);
                } else {
                    desplegarAsignaturas(new ArrayList<>());
                }
            }
            @Override public void onFailure(Call<List<Asignatura>> call, Throwable t) {
                desplegarAsignaturas(new ArrayList<>());
            }
        });
    }

    /**
     * Pobla la lista de asignaturas para su selección.
     */
    private void desplegarAsignaturas(List<Asignatura> asignaturas) {
        LinearLayout container = findViewById(R.id.llAsignaturaListContainer);
        ImageView arrow = findViewById(R.id.ivArrowAsignatura);
        if (container == null || arrow == null) return;

        container.setVisibility(View.VISIBLE);
        arrow.animate().rotation(180).setDuration(200).start();
        container.removeAllViews();

        View itemGeneral = LayoutInflater.from(this).inflate(R.layout.item_selector_opcion, container, false);
        TextView tvNombreGen = itemGeneral.findViewById(R.id.tvOpcionNombre);
        if (tvNombreGen != null) tvNombreGen.setText("General (Toda la clase)");
        itemGeneral.setOnClickListener(v -> {
            asignaturaSeleccionada = null;
            actualizarUIFiltros();
            container.setVisibility(View.GONE);
            arrow.animate().rotation(0).setDuration(200).start();
        });
        container.addView(itemGeneral);

        for (Asignatura asig : asignaturas) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_selector_opcion, container, false);
            TextView tvNombre = itemView.findViewById(R.id.tvOpcionNombre);
            View dot = itemView.findViewById(R.id.viewColorInd);

            if (tvNombre != null) tvNombre.setText(asig.getNombre());
            if (asig.getColor() != null && dot != null) {
                dot.setVisibility(View.VISIBLE);
                try {
                    dot.getBackground().setTint(Color.parseColor(asig.getColor()));
                } catch (Exception ignored) {}
            }

            itemView.setOnClickListener(v -> {
                asignaturaSeleccionada = asig;
                actualizarUIFiltros();
                container.setVisibility(View.GONE);
                arrow.animate().rotation(0).setDuration(200).start();
            });
            container.addView(itemView);
        }
    }

    /**
     * Establece el evento como privado (sin grupo ni asignatura).
     */
    private void seleccionarPrivado() {
        grupoSeleccionado = null;
        asignaturaSeleccionada = null;
        actualizarUIFiltros();
        findViewById(R.id.llClassListContainer).setVisibility(View.GONE);
        findViewById(R.id.ivArrowClass).setRotation(0);
        findViewById(R.id.llAsignaturaListContainer).setVisibility(View.GONE);
        findViewById(R.id.ivArrowAsignatura).setRotation(0);
    }

    /**
     * Actualiza el estado visual de los selectores de visibilidad.
     */
    private void actualizarUIFiltros() {
        MaterialCardView btnPrivate = findViewById(R.id.btnVisibilityPrivate);
        TextView tvPrivate = findViewById(R.id.tvVisibilityPrivate);
        MaterialCardView cardClass = findViewById(R.id.cardVisibilityClass);
        TextView tvClass = findViewById(R.id.tvVisibilityClass);
        MaterialCardView cardAsignatura = findViewById(R.id.cardVisibilityAsignatura);
        TextView tvAsignatura = findViewById(R.id.tvVisibilityAsignatura);

        if (btnPrivate == null || cardClass == null || cardAsignatura == null) return;

        resetVisibilityCard(btnPrivate, tvPrivate, "Solo para mí");
        resetVisibilityCard(cardClass, tvClass, "Seleccionar clase");
        resetVisibilityCard(cardAsignatura, tvAsignatura, "Seleccionar asignatura");

        if (grupoSeleccionado == null) {
            highlightVisibilityCard(btnPrivate, tvPrivate);
            cardClass.setVisibility(cachedGrupos.isEmpty() ? View.GONE : View.VISIBLE);
            cardAsignatura.setVisibility(View.GONE);
        } else {
            highlightVisibilityCard(cardClass, tvClass);
            tvClass.setText(grupoSeleccionado.getNombre());
            cardClass.setVisibility(View.VISIBLE);
            cardAsignatura.setVisibility(View.VISIBLE);

            if (asignaturaSeleccionada != null) {
                highlightVisibilityCard(cardAsignatura, tvAsignatura);
                tvAsignatura.setText(asignaturaSeleccionada.getNombre());
            } else {
                tvAsignatura.setText("General (Toda la clase)");
            }
        }
    }

    private void highlightVisibilityCard(MaterialCardView card, TextView text) {
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.tag_admin_bg));
        card.setStrokeWidth(3);
        card.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
        text.setTextColor(ContextCompat.getColor(this, R.color.primary));
    }

    private void resetVisibilityCard(MaterialCardView card, TextView text, String defaultText) {
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_card));
        card.setStrokeWidth(0);
        text.setTextColor(ContextCompat.getColor(this, R.color.text_sub));
        if (defaultText != null) text.setText(defaultText);
    }

    /**
     * Configura los selectores de tipo de tarea (Chips).
     */
    private void configurarChips() {
        View.OnClickListener listener = v -> {
            resetChips();
            MaterialCardView selected = (MaterialCardView) v;
            int colorFondo, colorBorde, colorTexto;

            if (v.getId() == R.id.chipDeberes) {
                categoriaSeleccionada = "Deberes";
                colorFondo = ContextCompat.getColor(this, R.color.tag_admin_bg);
                colorBorde = ContextCompat.getColor(this, R.color.primary);
                colorTexto = ContextCompat.getColor(this, R.color.primary);
            } else if (v.getId() == R.id.chipProyecto) {
                categoriaSeleccionada = "Proyecto";
                colorFondo = ContextCompat.getColor(this, R.color.tag_proyecto_bg);
                colorBorde = ContextCompat.getColor(this, R.color.tag_proyecto_text);
                colorTexto = ContextCompat.getColor(this, R.color.tag_proyecto_text);
            } else {
                categoriaSeleccionada = "Examen";
                colorFondo = Color.parseColor("#20EF4444");
                colorBorde = Color.parseColor("#EF4444");
                colorTexto = Color.parseColor("#EF4444");
            }

            selected.setCardBackgroundColor(colorFondo);
            selected.setStrokeWidth(3);
            selected.setStrokeColor(colorBorde);

            LinearLayout layout = (LinearLayout) selected.getChildAt(0);
            View punto = layout.getChildAt(0);
            TextView texto = (TextView) layout.getChildAt(1);

            punto.getBackground().setTint(colorBorde);
            texto.setTextColor(colorTexto);
        };

        chipDeberes.setOnClickListener(listener);
        chipProyecto.setOnClickListener(listener);
        chipExamen.setOnClickListener(listener);

        seleccionarChipPorTipo(categoriaSeleccionada);
    }

    private void resetChips() {
        int bgNormal = Color.WHITE;
        int textGray = Color.parseColor("#757575");
        int borderColor = Color.parseColor("#E0E0E0");

        MaterialCardView[] chips = {chipDeberes, chipProyecto, chipExamen};
        int[] puntosColores = {
                Color.parseColor("#3F51B5"),
                Color.parseColor("#FBC02D"),
                Color.parseColor("#EF4444")
        };

        for (int i = 0; i < chips.length; i++) {
            chips[i].setCardBackgroundColor(bgNormal);
            chips[i].setStrokeWidth(2);
            chips[i].setStrokeColor(borderColor);
            chips[i].setCardElevation(2);

            LinearLayout layout = (LinearLayout) chips[i].getChildAt(0);
            View punto = layout.getChildAt(0);
            TextView texto = (TextView) layout.getChildAt(1);

            punto.getBackground().setTint(puntosColores[i]);
            texto.setTextColor(textGray);
        }
    }

    private void añadirSubtareaVacia() {
        listaSubtareas.add(new Subtarea(""));
        actualizarUIListaTareas();
    }

    /**
     * Carga los datos de un evento existente para su edición.
     */
    private void cargarDatosEventoEdicion() {
        apiService.obtenerEventoPorId(eventoEdicionId).enqueue(new Callback<Evento>() {
            @Override
            public void onResponse(@NonNull Call<Evento> call, @NonNull Response<Evento> response) {
                if (response.isSuccessful() && response.body() != null) {
                    eventoOriginal = response.body();
                    poblarCampos(eventoOriginal);
                }
            }
            @Override public void onFailure(@NonNull Call<Evento> call, @NonNull Throwable t) {}
        });
    }

    /**
     * Rellena los campos de la interfaz con la información de un objeto Evento.
     */
    private void poblarCampos(Evento e) {
        etEventTitle.setText(e.getTitulo());
        etEventNotes.setText(e.getDescripcion());
        categoriaSeleccionada = e.getTipo();
        grupoSeleccionado = e.getGrupo();
        asignaturaSeleccionada = e.getAsignatura();
        seleccionarChipPorTipo(e.getTipo());

        actualizarUIFiltros();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            calendarInicio.setTime(sdf.parse(e.getFecha()));
            calendarFin.setTime(sdf.parse(e.getFecha()));

            SimpleDateFormat sdfH = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String horaStr = e.getHora();
            if (horaStr != null && horaStr.contains("-")) {
                String[] partes = horaStr.split("-");
                Calendar auxInicio = Calendar.getInstance();
                auxInicio.setTime(sdfH.parse(partes[0].trim()));
                calendarInicio.set(Calendar.HOUR_OF_DAY, auxInicio.get(Calendar.HOUR_OF_DAY));
                calendarInicio.set(Calendar.MINUTE, auxInicio.get(Calendar.MINUTE));

                if (partes.length > 1) {
                    Calendar auxFin = Calendar.getInstance();
                    auxFin.setTime(sdfH.parse(partes[1].trim()));
                    calendarFin.set(Calendar.HOUR_OF_DAY, auxFin.get(Calendar.HOUR_OF_DAY));
                    calendarFin.set(Calendar.MINUTE, auxFin.get(Calendar.MINUTE));
                }
            }
        } catch (Exception ignored) {}

        final boolean focusValue = e.isFocusMode();
        final boolean dndValue = e.isDndEnabled();
        if (switchPomodoro != null) {
            switchPomodoro.post(() -> {
                switchPomodoro.setChecked(focusValue);
                if (llDndOptions != null) llDndOptions.setVisibility(focusValue ? View.VISIBLE : View.GONE);
                SwitchCompat switchDnd = findViewById(R.id.switchDnd);
                if (switchDnd != null) switchDnd.setChecked(dndValue);
            });
        }

        listaSubtareas.clear();
        if (e.getSubtareas() != null) listaSubtareas.addAll(e.getSubtareas());

        actualizarTextosFechaHora();
        actualizarUIListaTareas();
    }

    private void seleccionarChipPorTipo(String tipo) {
        resetChips();
        MaterialCardView selected;
        int colorFondo, colorBorde, colorTexto;

        if ("Proyecto".equals(tipo)) {
            selected = chipProyecto;
            colorFondo = ContextCompat.getColor(this, R.color.tag_proyecto_bg);
            colorBorde = ContextCompat.getColor(this, R.color.tag_proyecto_text);
            colorTexto = ContextCompat.getColor(this, R.color.tag_proyecto_text);
        } else if ("Examen".equals(tipo)) {
            selected = chipExamen;
            colorFondo = Color.parseColor("#20EF4444");
            colorBorde = Color.parseColor("#EF4444");
            colorTexto = Color.parseColor("#EF4444");
        } else {
            selected = chipDeberes;
            colorFondo = ContextCompat.getColor(this, R.color.tag_admin_bg);
            colorBorde = ContextCompat.getColor(this, R.color.primary);
            colorTexto = ContextCompat.getColor(this, R.color.primary);
        }

        selected.setCardBackgroundColor(colorFondo);
        selected.setStrokeWidth(3);
        selected.setStrokeColor(colorBorde);

        LinearLayout layout = (LinearLayout) selected.getChildAt(0);
        View punto = layout.getChildAt(0);
        TextView texto = (TextView) layout.getChildAt(1);

        punto.getBackground().setTint(colorBorde);
        texto.setTextColor(colorTexto);
    }

    /**
     * Renderiza dinámicamente la lista de subtareas.
     */
    private void actualizarUIListaTareas() {
        llTaskList.removeAllViews();
        for (int i = 0; i < listaSubtareas.size(); i++) {
            Subtarea subtareaActual = listaSubtareas.get(i);
            View view = LayoutInflater.from(this).inflate(R.layout.item_subtarea, llTaskList, false);
            EditText et = view.findViewById(R.id.etSubtitulo);
            ImageView btnDelete = view.findViewById(R.id.btnEditSubtarea);

            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                listaSubtareas.remove(subtareaActual);
                actualizarUIListaTareas();
            });

            et.setText(subtareaActual.getDescripcion());
            et.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override 
                public void onTextChanged(CharSequence s, int start, int before, int count) { 
                    subtareaActual.setDescripcion(s.toString());
                    subtareaActual.setTitulo(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            llTaskList.addView(view);
        }
    }

    /**
     * Pobla las opciones disponibles de recordatorio según la duración del evento.
     */
    private void poblarListaRecordatorios(LinearLayout container, ImageView arrow) {
        container.removeAllViews();
        List<String> opciones = new ArrayList<>();
        opciones.add("Desactivado");
        opciones.add("En el momento");
        
        long diffMs = calendarFin.getTimeInMillis() - calendarInicio.getTimeInMillis();
        long duracionMinutos = diffMs / 60000;

        if (duracionMinutos >= 15) opciones.add("15 minutos antes");
        if (duracionMinutos >= 60) opciones.add("1 hora antes");
        
        for (String opcion : opciones) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_selector_opcion, container, false);
            TextView tvNombre = itemView.findViewById(R.id.tvOpcionNombre);
            if (tvNombre != null) tvNombre.setText(opcion);

            itemView.setOnClickListener(v -> {
                tvReminderValue.setText(opcion);
                container.setVisibility(View.GONE);
                arrow.animate().rotation(0).setDuration(200).start();
            });
            container.addView(itemView);
        }
    }

    private void mostrarDatePicker(Calendar cal, boolean esInicio) {
        new DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            validarFechas(esInicio);
            actualizarTextosFechaHora();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarTimePicker(Calendar cal, boolean esInicio) {
        new TimePickerDialog(this, (view, h, min) -> {
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, min);
            validarFechas(esInicio);
            actualizarTextosFechaHora();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    /**
     * Asegura que la fecha/hora de finalización sea siempre posterior a la de inicio.
     */
    private void validarFechas(boolean esInicio) {
        if (calendarFin.before(calendarInicio)) {
            if (esInicio) {
                calendarFin.setTime(calendarInicio.getTime());
                calendarFin.add(Calendar.HOUR_OF_DAY, 1);
            } else {
                calendarInicio.setTime(calendarFin.getTime());
                calendarInicio.add(Calendar.HOUR_OF_DAY, -1);
            }
        }
    }

    private void validarRecordatorioPorDuracion() {
        if (tvReminderValue == null) return;
        String valorActual = tvReminderValue.getText().toString();
        long diffMs = calendarFin.getTimeInMillis() - calendarInicio.getTimeInMillis();
        long duracionMinutos = diffMs / 60000;

        if (valorActual.equals("1 hora antes") && duracionMinutos < 60) tvReminderValue.setText("En el momento");
        else if (valorActual.equals("15 minutos antes") && duracionMinutos < 15) tvReminderValue.setText("En el momento");
    }

    private void actualizarTextosFechaHora() {
        SimpleDateFormat sdfD = new SimpleDateFormat("dd MMM, yyyy", new Locale("es", "ES"));
        SimpleDateFormat sdfT = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvStartDate.setText(sdfD.format(calendarInicio.getTime()));
        tvStartTime.setText(sdfT.format(calendarInicio.getTime()));
        tvEndDate.setText(sdfD.format(calendarFin.getTime()));
        tvEndTime.setText(sdfT.format(calendarFin.getTime()));
        validarRecordatorioPorDuracion();
    }

    /**
     * Recopila los datos del formulario y los envía al servidor para guardar el evento.
     */
    private void guardarEvento() {
        String titulo = etEventTitle.getText().toString().trim();
        if (titulo.isEmpty()) {
            etEventTitle.setError("El título es obligatorio");
            return;
        }

        btnSave.setEnabled(false);

        Evento e = (eventoOriginal != null) ? eventoOriginal : new Evento();
        e.setTitulo(titulo);
        e.setDescripcion(etEventNotes.getText().toString());
        e.setFecha(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarInicio.getTime()));
        
        String horaInicio = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendarInicio.getTime());
        String horaFin = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendarFin.getTime());
        e.setHora(horaInicio + "-" + horaFin);
        e.setTipo(categoriaSeleccionada);
        e.setSubtareas(new ArrayList<>(listaSubtareas));
        
        boolean modoConcentracionActivo = (switchPomodoro != null && switchPomodoro.isChecked());
        e.setFocusMode(modoConcentracionActivo);
        
        SwitchCompat switchDnd = findViewById(R.id.switchDnd);
        if (switchDnd != null) e.setDndEnabled(switchDnd.isChecked());

        e.setGrupo(grupoSeleccionado);
        e.setAsignatura(asignaturaSeleccionada);
        
        if (e.getCreador() == null) {
            Usuario u = new Usuario(); u.setId(session.getUserId());
            e.setCreador(u);
        }

        Call<Evento> call = (eventoEdicionId != -1L) ? apiService.actualizarEvento(eventoEdicionId, e) : apiService.crearEvento(e);

        call.enqueue(new Callback<Evento>() {
            @Override
            public void onResponse(@NonNull Call<Evento> call, @NonNull Response<Evento> response) {
                if (response.isSuccessful() && response.body() != null) {
                    programarNotificacion(response.body());
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnSave.setEnabled(true);
                    Toast.makeText(FormularioActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Evento> call, @NonNull Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(FormularioActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Programa una notificación local utilizando AlarmManager basada en la configuración del usuario.
     * 
     * @param evento Evento para el cual programar la notificación.
     */
    private void programarNotificacion(Evento evento) {
        if (!session.recordatoriosActivos()) return;
        
        String recordatorioStr = tvReminderValue.getText().toString();
        if ("Desactivado".equals(recordatorioStr)) return;

        int minutosAntes = 0;
        if (recordatorioStr.equals("En el momento")) minutosAntes = 0;
        else if (recordatorioStr.contains("15 minutos")) minutosAntes = 15;
        else if (recordatorioStr.contains("1 hora")) minutosAntes = 60;
        else return;

        if (minutosAntes >= 0) {
            String tituloNotif;
            String contenidoNotif;

            if (minutosAntes == 0) {
                tituloNotif = "Tarea finalizada";
                contenidoNotif = "La tarea \"" + evento.getTitulo() + "\" ha llegado a su hora de finalización";
            } else {
                tituloNotif = "Recordatorio de finalización";
                String tiempoLabel = recordatorioStr.toLowerCase().replace("antes", "").trim();
                contenidoNotif = "Quedan " + tiempoLabel + " para que acabe \"" + evento.getTitulo() + "\"";
            }

            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
            android.content.Intent intent = new android.content.Intent(this, com.example.agendaacademica.NotificacionReceiver.class);
            intent.putExtra("EVENTO_ID", evento.getId());
            intent.putExtra("TITULO", tituloNotif);
            intent.putExtra("CONTENIDO", contenidoNotif);

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    this,
                    evento.getId().intValue(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calAlarma = (Calendar) calendarFin.clone();
            calAlarma.add(Calendar.MINUTE, -minutosAntes);
            calAlarma.set(Calendar.SECOND, 0);

            Calendar ahora = Calendar.getInstance();

            if (calAlarma.after(ahora)) {
                long tiempoAlarma = calAlarma.getTimeInMillis();
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            tiempoAlarma,
                            pendingIntent
                    );
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        intentarCerrar();
    }

    /**
     * Verifica si hay cambios sin guardar antes de cerrar la actividad.
     */
    private void intentarCerrar() {
        if (hayCambiosSinGuardar()) {
            mostrarDialogoSalida();
        } else {
            finish();
        }
    }

    private boolean hayCambiosSinGuardar() {
        String titulo = etEventTitle.getText().toString().trim();
        String notas = etEventNotes.getText().toString().trim();

        if (eventoEdicionId == -1L) {
            return !titulo.isEmpty() || !notas.isEmpty() || !listaSubtareas.isEmpty();
        } else {
            if (eventoOriginal == null) return false;
            String originalTitulo = eventoOriginal.getTitulo() != null ? eventoOriginal.getTitulo() : "";
            String originalNotas = eventoOriginal.getDescripcion() != null ? eventoOriginal.getDescripcion() : "";
            return !titulo.equals(originalTitulo) || !notas.equals(originalNotas);
        }
    }

    /**
     * Muestra un diálogo de confirmación si el usuario intenta salir con cambios sin guardar.
     */
    private void mostrarDialogoSalida() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambios sin guardar")
                .setMessage("¿Estás seguro de que quieres salir? Se perderán los cambios.")
                .setPositiveButton("Salir", (dialog, which) -> finish())
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
