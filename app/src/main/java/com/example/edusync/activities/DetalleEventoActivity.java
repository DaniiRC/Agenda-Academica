package com.example.edusync.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.example.edusync.model.Evento;
import com.example.edusync.model.Subtarea;
import com.example.edusync.network.RetrofitClient;
import com.example.edusync.viewmodel.EventoViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad que muestra los detalles exhaustivos de un evento seleccionado.
 * Permite visualizar el estado de progreso, gestionar subtareas, editar información,
 * eliminar el evento y marcarlo como completado. Soporta funcionamiento offline
 * sincronizado con los datos del servidor.
 */
public class DetalleEventoActivity extends BaseActivity {

    private TextView tvEventTitle, tvEventBadge, tvEventDate, tvEventTimeStart, tvEventTimeEnd, tvDash, tvProgressPercent, tvNotesContent, tvNotaValor;
    private ImageView btnBack, btnEdit, btnDelete;
    private ProgressBar pbStudyPlan;
    private LinearLayout llStudyPlanContainer, llNotaSection;
    private MaterialButton btnCompletarEvento, btnEditarNota;
    private Long eventoId;
    private Evento eventoActual;
    private SessionManager session;
    private EventoViewModel eventoViewModel;
    private SwipeRefreshLayout swipeRefresh;
    private boolean isAdmin;

    // Vistas del temporizador Pomodoro
    private com.google.android.material.card.MaterialCardView cardPomodoro;
    private TextView tvPomodoroSeconds;
    private android.widget.EditText etPomodoroMinutes;
    private ProgressBar pbPomodoro;
    private MaterialButton btnStartPomodoro;
    private ImageView btnResetPomodoro;
    private android.os.CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long totalTimeInMillis = 25 * 60 * 1000;
    private long originalTimeInMillis = 25 * 60 * 1000;

    /**
     * Inicializa la actividad, recupera los parámetros de navegación y configura los observadores de datos.
     * 
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_evento);

        session = new SessionManager(this);
        eventoId = getIntent().getLongExtra("EVENTO_ID", -1);
        boolean esPasada = getIntent().getBooleanExtra("ES_PASADA", false);
        this.isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        initViews();
        configurarBotones(esPasada, isAdmin);

        eventoViewModel = new androidx.lifecycle.ViewModelProvider(this).get(EventoViewModel.class);
        observarDatosLocal();
    }

    /**
     * Establece un observador sobre la base de datos local (Room) para reflejar cambios en tiempo real.
     */
    private void observarDatosLocal() {
        eventoViewModel.getEventoPorId(eventoId).observe(this, entity -> {
            if (entity != null) {
                if (eventoActual == null) {
                    eventoActual = new Evento();
                    eventoActual.setId(entity.getId());
                }
                eventoActual.setTitulo(entity.getTitulo());
                eventoActual.setDescripcion(entity.getDescripcion());
                eventoActual.setFecha(entity.getFecha());
                eventoActual.setHora(entity.getHora());
                eventoActual.setCompletado(entity.isCompletado());
                
                poblarInterfaz(entity);
            }
        });
    }

    /**
     * Vincula los componentes de la interfaz con las variables de la clase.
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventBadge = findViewById(R.id.tvEventBadge);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventTimeStart = findViewById(R.id.tvEventTimeStart);
        tvEventTimeEnd = findViewById(R.id.tvEventTimeEnd);
        tvDash = findViewById(R.id.tvDash);
        pbStudyPlan = findViewById(R.id.pbStudyPlan);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        llStudyPlanContainer = findViewById(R.id.llSubtareasList);
        tvNotesContent = findViewById(R.id.tvNotesContent);
        btnCompletarEvento = findViewById(R.id.btnCompletarEvento);
        tvNotaValor = findViewById(R.id.tvNotaValor);
        btnEditarNota = findViewById(R.id.btnEditarNota);
        llNotaSection = findViewById(R.id.llNotaSection);
        swipeRefresh = findViewById(R.id.swipeRefreshDetalle);
        
        // Vistas del temporizador Pomodoro
        cardPomodoro = findViewById(R.id.cardPomodoro);
        etPomodoroMinutes = findViewById(R.id.etPomodoroMinutes);
        tvPomodoroSeconds = findViewById(R.id.tvPomodoroSeconds);
        pbPomodoro = findViewById(R.id.pbPomodoro);
        btnStartPomodoro = findViewById(R.id.btnStartPomodoro);
        btnResetPomodoro = findViewById(R.id.btnResetPomodoro);
        
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::cargarDatos);
        }
    }

    /**
     * Configura la visibilidad y funcionalidad de los botones de acción según el rol y estado del evento.
     * 
     * @param esPasada Indica si la fecha del evento ya ha transcurrido.
     * @param isAdmin  Indica si el usuario actual tiene permisos de edición sobre el evento.
     */
    private void configurarBotones(boolean esPasada, boolean isAdmin) {
        this.isAdmin = isAdmin;
        btnBack.setOnClickListener(v -> finish());
        
        boolean isCompletada = (eventoActual != null && eventoActual.isCompletado());
        // Un alumno no puede completar un evento de grupo: solo el admin lo hace para todos
        boolean esEventoDeGrupo = (eventoActual != null && eventoActual.getGrupo() != null);
        boolean alumnoEnGrupo = esEventoDeGrupo && !isAdmin;

        if (esPasada || !isAdmin || isCompletada) {
            btnDelete.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
            
            if (esPasada || isCompletada || alumnoEnGrupo) {
                // Ocultar completar: pasada, ya completada, o alumno en tarea de grupo
                btnCompletarEvento.setVisibility(View.GONE);
            } else {
                btnCompletarEvento.setOnClickListener(v -> completarEvento());
            }
        } else {
            btnDelete.setOnClickListener(v -> confirmarEliminacion());
            btnEdit.setOnClickListener(v -> irAEditar());
            btnCompletarEvento.setOnClickListener(v -> completarEvento());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventoId != -1) cargarDatos();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isTimerRunning) {
            pararTimer();
        }
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        eventoId = intent.getLongExtra("EVENTO_ID", -1);
        cargarDatos();
    }

    /**
     * Recupera los datos actualizados del evento desde el servidor API.
     */
    private void cargarDatos() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        RetrofitClient.getApiService().obtenerEventoPorId(eventoId).enqueue(new Callback<Evento>() {
            @Override
            public void onResponse(Call<Evento> call, Response<Evento> response) {
                if (response.isSuccessful() && response.body() != null) {
                    eventoActual = response.body();
                    
                    // Recalcular los permisos dinámicamente según el creador
                    if (eventoActual.getCreador() != null) {
                        isAdmin = eventoActual.getCreador().getId().equals(session.getUserId());
                    }
                    
                    // Actualizar botones de la barra superior (Editar / Borrar)
                    configurarBotones(getIntent().getBooleanExtra("ES_PASADA", false), isAdmin);

                    if (eventoActual.getGrupo() != null && !isAdmin) {
                        cargarNotaIndividual();
                    } else {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        poblarInterfaz(eventoActual);
                    }
                } else {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }
            }
            @Override public void onFailure(Call<Evento> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    /**
     * Rellena la interfaz utilizando un objeto de dominio Evento.
     * 
     * @param evento El objeto con los datos a mostrar.
     */
    private void poblarInterfaz(Evento evento) {
        tvEventTitle.setText(evento.getTitulo());
        
        String badgeText = "";
        if (evento.getTipo() != null && !evento.getTipo().isEmpty()) {
            if ("Deberes".equalsIgnoreCase(evento.getTipo())) badgeText = getString(R.string.deberes_mayus);
            else if ("Proyecto".equalsIgnoreCase(evento.getTipo())) badgeText = getString(R.string.proyecto_mayus);
            else if ("Examen".equalsIgnoreCase(evento.getTipo())) badgeText = getString(R.string.examen_mayus);
            else badgeText = evento.getTipo().toUpperCase();
        }

        if (evento.getGrupo() != null) {
            String groupName = evento.getGrupo().getNombre();
            badgeText = badgeText.isEmpty() ? groupName : badgeText + " • " + groupName;
        } else if (evento.getAsignatura() != null) {
            String asigName = evento.getAsignatura().getNombre();
            badgeText = badgeText.isEmpty() ? asigName : badgeText + " • " + asigName;
        } else {
            // La tarea es privada (sin grupo ni asignatura), se indica explícitamente.
            String soloParaMi = getString(R.string.solo_para_mi).toUpperCase();
            badgeText = badgeText.isEmpty() ? soloParaMi : badgeText + " • " + soloParaMi;
        }
        tvEventBadge.setText(badgeText);
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(evento.getFecha());
            tvEventDate.setText(new SimpleDateFormat("dd MMM", Locale.getDefault()).format(date));
        } catch (Exception e) {
            tvEventDate.setText(evento.getFecha());
        }

        configurarHorario(evento.getHora());
            
        String desc = evento.getDescripcion();
        tvNotesContent.setText(desc != null && !desc.trim().isEmpty() ? desc : getString(R.string.sin_info_adicional));
        tvNotesContent.setTypeface(null, desc != null && !desc.trim().isEmpty() ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.ITALIC);

        if (evento.getAsignatura() != null) {
            llNotaSection.setVisibility(View.VISIBLE);
            
            if (this.isAdmin && evento.getGrupo() != null) {
                // Modo Admin para grupo: ver lista de alumnos
                tvNotaValor.setText(R.string.calificar_actividad);
                btnEditarNota.setVisibility(View.VISIBLE);
                btnEditarNota.setText("Ver calificaciones");
                btnEditarNota.setOnClickListener(v -> irACalificarAlumnos());
            } else {
                // Modo Alumno o Tarea Personal
                if (evento.getNotaObtenida() != null) {
                    tvNotaValor.setText(String.format(Locale.getDefault(), "%.2f", evento.getNotaObtenida()));
                    btnEditarNota.setText(R.string.cambiar_nota);
                } else {
                    tvNotaValor.setText(R.string.sin_calificar);
                    btnEditarNota.setText(R.string.poner_nota);
                }
                
                if (this.isAdmin) { // Tarea personal (no grupo)
                    btnEditarNota.setVisibility(View.VISIBLE);
                    btnEditarNota.setOnClickListener(v -> mostrarDialogoNota());
                } else {
                    btnEditarNota.setVisibility(View.GONE);
                }
            }
        } else {
            llNotaSection.setVisibility(View.GONE);
        }

        // Configurar Pomodoro si el modo concentración está activo
        if (evento.isFocusMode()) {
            cardPomodoro.setVisibility(View.VISIBLE);
            btnStartPomodoro.setOnClickListener(v -> toggleTimer());
            btnResetPomodoro.setOnClickListener(v -> reiniciarTimer());
            
            etPomodoroMinutes.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && !isTimerRunning) {
                    try {
                        int mins = Integer.parseInt(etPomodoroMinutes.getText().toString());
                        totalTimeInMillis = (long) mins * 60 * 1000;
                        originalTimeInMillis = totalTimeInMillis;
                        actualizarUIPomodoro();
                    } catch (Exception e) {
                        etPomodoroMinutes.setText("25");
                    }
                }
            });
        } else {
            cardPomodoro.setVisibility(View.GONE);
        }

        // El botón de completar se muestra siempre que el evento no esté completado.
        // Si hay subtareas, actualizarProgreso() se encarga de mostrarlo/ocultarlo según el avance.
        if (evento.isCompletado()) {
            btnCompletarEvento.setVisibility(View.GONE);
        } else if (evento.getSubtareas() == null || evento.getSubtareas().isEmpty()) {
            btnCompletarEvento.setVisibility(View.VISIBLE);
        }

        construirSubtareas(evento.getSubtareas());

        // Usar el isAdmin dinámico (ya calculado en cargarDatos), no el del Intent
        boolean esPasada = getIntent().getBooleanExtra("ES_PASADA", false);
        configurarBotones(esPasada, this.isAdmin);
    }

    /**
     * Rellena la interfaz utilizando una entidad de base de datos local.
     * 
     * @param entity La entidad almacenada en Room.
     */
    private void poblarInterfaz(com.example.edusync.database.EventoEntity entity) {
        tvEventTitle.setText(entity.getTitulo());
        tvEventBadge.setText(R.string.local_mayus);
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(entity.getFecha());
            tvEventDate.setText(new SimpleDateFormat("dd MMM", Locale.getDefault()).format(date));
        } catch (Exception e) {
            tvEventDate.setText(entity.getFecha());
        }

        configurarHorario(entity.getHora());
        tvNotesContent.setText(entity.getDescripcion() != null && !entity.getDescripcion().isEmpty() ? entity.getDescripcion() : getString(R.string.sin_info_adicional));
        tvNotesContent.setTypeface(null, entity.getDescripcion() != null && !entity.getDescripcion().isEmpty() ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.ITALIC);
        btnCompletarEvento.setVisibility(View.GONE);
    }

    /**
     * Procesa y muestra el rango horario del evento.
     * 
     * @param horaStr Cadena de texto con el formato de hora "HH:mm" o "HH:mm-HH:mm".
     */
    private void configurarHorario(String horaStr) {
        if (horaStr != null && !horaStr.isEmpty()) {
            if (horaStr.contains("-")) {
                String[] horas = horaStr.split("-");
                tvEventTimeStart.setText(formatTime(horas[0]));
                if (horas.length > 1) {
                    tvEventTimeEnd.setText(formatTime(horas[1]));
                    tvEventTimeEnd.setVisibility(View.VISIBLE);
                    tvDash.setVisibility(View.VISIBLE);
                }
            } else {
                tvEventTimeStart.setText(formatTime(horaStr));
                tvEventTimeEnd.setVisibility(View.GONE);
                tvDash.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Genera dinámicamente las vistas para las subtareas del evento.
     * 
     * @param subtareas Lista de subtareas asociadas.
     */
    private void construirSubtareas(List<Subtarea> subtareas) {
        llStudyPlanContainer.removeAllViews();
        View sectionPlan = findViewById(R.id.llStudyPlanSection);

        if (subtareas == null || subtareas.isEmpty()) {
            if (sectionPlan != null) sectionPlan.setVisibility(View.GONE);
            if (eventoActual != null && !eventoActual.isCompletado()) {
                btnCompletarEvento.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (sectionPlan != null) sectionPlan.setVisibility(View.VISIBLE);
        for (Subtarea sub : subtareas) {
            View taskView = getLayoutInflater().inflate(R.layout.item_subtarea, llStudyPlanContainer, false);
            TextView tv = taskView.findViewById(R.id.etSubtitulo);
            View container = taskView.findViewById(R.id.llTaskContainer);
            View card = taskView.findViewById(R.id.cardSubtarea);
            ImageView ivCheck = taskView.findViewById(R.id.ivCheck);

            tv.setText(sub.getTitulo());
            
            // El campo de texto de la subtarea es de solo lectura; el clic se delega al contenedor.
            tv.setFocusable(false);
            tv.setClickable(false);
            tv.setCursorVisible(false);
            
            // Cualquier zona de la tarjeta (icono, texto o fondo) activa el mismo listener de clic.
            View.OnClickListener taskClickListener = v -> container.performClick();
            tv.setOnClickListener(taskClickListener);
            card.setOnClickListener(taskClickListener);
            ivCheck.setOnClickListener(taskClickListener);
            
            actualizarVisualSubtarea(sub, tv, card, ivCheck);

            container.setOnClickListener(v -> {
                boolean nextState = !sub.isCompletada();
                sub.setCompletada(nextState);
                actualizarVisualSubtarea(sub, tv, card, ivCheck);
                actualizarSubtareaEnServidor(sub.getId(), nextState);
                actualizarProgreso(subtareas);
            });
            llStudyPlanContainer.addView(taskView);
        }
        actualizarProgreso(subtareas);
    }

    /**
     * Actualiza el aspecto visual de una subtarea individual basándose en su estado.
     */
    private void actualizarVisualSubtarea(Subtarea sub, TextView tv, View card, ImageView ivCheck) {
        if (ivCheck != null) ivCheck.setVisibility(sub.isCompletada() ? View.VISIBLE : View.INVISIBLE);
        if (sub.isCompletada()) {
            tv.setPaintFlags(tv.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.5f);
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card).setStrokeColor(Color.parseColor("#10B981"));
                card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D10B981")));
            }
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setAlpha(1.0f);
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card).setStrokeColor(Color.parseColor("#33FFFFFF"));
                card.setBackgroundTintList(null);
            }
        }
    }

    /**
     * Calcula y muestra el porcentaje de completitud del evento basándose en sus subtareas.
     * 
     * @param subtareas Lista de subtareas para el cálculo.
     */
    private void actualizarProgreso(List<Subtarea> subtareas) {
        if (subtareas == null || subtareas.isEmpty()) {
            if (eventoActual != null && !eventoActual.isCompletado()) btnCompletarEvento.setVisibility(View.VISIBLE);
            return;
        }

        int completadas = 0;
        for (Subtarea s : subtareas) if (s.isCompletada()) completadas++;
        
        int pct = (completadas * 100) / subtareas.size();
        pbStudyPlan.setProgress(pct);
        tvProgressPercent.setText(getString(R.string.completado_pct, pct));

        if (pct == 100 && eventoActual != null && !eventoActual.isCompletado()) {
            if (btnCompletarEvento.getVisibility() != View.VISIBLE) {
                btnCompletarEvento.setVisibility(View.VISIBLE);
                btnCompletarEvento.setAlpha(0f);
                btnCompletarEvento.animate().alpha(1f).setDuration(500).start();
            }
        } else {
            btnCompletarEvento.setVisibility(View.GONE);
        }
    }

    /**
     * Sincroniza el estado de una subtarea con el servidor API.
     * 
     * @param id         ID único de la subtarea.
     * @param completada Nuevo estado de completitud.
     */
    private void actualizarSubtareaEnServidor(Long id, boolean completada) {
        if (id == null || id == -1) return;
        RetrofitClient.getApiService().actualizarSubtarea(id, completada).enqueue(new Callback<Void>() {
            @Override 
            public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override 
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    /**
     * Marca el evento completo en el servidor y otorga puntos de experiencia al usuario.
     */
    private void completarEvento() {
        btnCompletarEvento.setEnabled(false);
        RetrofitClient.getApiService().marcarEventoCompletado(eventoId, session.getUserId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DetalleEventoActivity.this, R.string.tarea_completada_toast, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnCompletarEvento.setEnabled(true);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnCompletarEvento.setEnabled(true);
            }
        });
    }

    /**
     * Navega a la actividad de edición del evento actual.
     */
    private void irAEditar() {
        Intent intent = new Intent(this, FormularioActivity.class);
        intent.putExtra("EVENTO_ID", eventoId);
        startActivity(intent);
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar permanentemente el evento.
     */
    private void confirmarEliminacion() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_evento_titulo)
            .setMessage(R.string.eliminar_evento_msg)
            .setPositiveButton(R.string.eliminar, (dialog, which) -> {
                RetrofitClient.getApiService().eliminarEvento(eventoId).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) { 
                        setResult(RESULT_OK);
                        finish(); 
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            })
            .setNegativeButton(R.string.cancelar, null)
            .show();
    }

    private void irACalificarAlumnos() {
        Intent intent = new Intent(this, CalificarAlumnosActivity.class);
        intent.putExtra("EVENTO_ID", eventoId);
        intent.putExtra("GRUPO_ID", eventoActual.getGrupo().getId());
        startActivity(intent);
    }

    private void cargarNotaIndividual() {
        RetrofitClient.getApiService().obtenerCalificacionDeUsuarioEnEvento(eventoId, session.getUserId()).enqueue(new Callback<com.example.edusync.model.Calificacion>() {
            @Override
            public void onResponse(Call<com.example.edusync.model.Calificacion> call, Response<com.example.edusync.model.Calificacion> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    eventoActual.setNotaObtenida(response.body().getNota());
                } else {
                    eventoActual.setNotaObtenida(null); // Sin nota
                }
                poblarInterfaz(eventoActual);
            }

            @Override
            public void onFailure(Call<com.example.edusync.model.Calificacion> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                poblarInterfaz(eventoActual);
            }
        });
    }

    private void mostrarDialogoNota() {
        android.widget.EditText etNota = new android.widget.EditText(this);
        etNota.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etNota.setHint("Ej: 8.5");
        
        if (eventoActual != null && eventoActual.getNotaObtenida() != null) {
            etNota.setText(String.valueOf(eventoActual.getNotaObtenida()));
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.calificar_actividad)
            .setMessage(R.string.introduce_nota)
            .setView(etNota)
            .setPositiveButton(R.string.guardar, (dialog, which) -> {
                String val = etNota.getText().toString().replace(",", ".");
                if (!val.isEmpty()) {
                    try {
                        double nota = Double.parseDouble(val);
                        if (nota < 0 || nota > 10) {
                            Toast.makeText(this, R.string.nota_rango_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        guardarNota(nota);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.nota_formato_error, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancelar, null)
            .show();
    }

    /**
     * Persiste la nota obtenida en el servidor y actualiza la vista local.
     *
     * @param nota Calificación a guardar (valor entre 0.0 y 10.0).
     */
    private void guardarNota(double nota) {
        RetrofitClient.getApiService().actualizarNota(eventoId, nota).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DetalleEventoActivity.this, R.string.nota_guardada_ok, Toast.LENGTH_SHORT).show();
                    tvNotaValor.setText(String.format(Locale.getDefault(), "%.2f", nota));
                    btnEditarNota.setText(R.string.cambiar_nota);
                    cargarDatos();
                } else {
                    String errorMsg = "Error al guardar nota (Código: " + response.code() + ")";
                    Toast.makeText(DetalleEventoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(DetalleEventoActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Formatea una cadena de hora eliminando los segundos si están presentes.
     * 
     * @param time Cadena de tiempo original.
     * @return Cadena formateada como "HH:mm".
     */
    private String formatTime(String time) {
        if (time == null) return "";
        time = time.trim();
        if (time.length() >= 8 && time.charAt(2) == ':' && time.charAt(5) == ':') {
            return time.substring(0, 5);
        }
        return time;
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            pararTimer();
        } else {
            iniciarTimer();
        }
    }

    private void iniciarTimer() {
        if (!isTimerRunning) {
            try {
                int mins = Integer.parseInt(etPomodoroMinutes.getText().toString());
                long targetTime = (long) mins * 60 * 1000;
                if (originalTimeInMillis != targetTime) {
                    originalTimeInMillis = targetTime;
                    totalTimeInMillis = targetTime;
                }
            } catch (Exception ignored) {}
        }

        etPomodoroMinutes.setEnabled(false);
        countDownTimer = new android.os.CountDownTimer(totalTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                totalTimeInMillis = millisUntilFinished;
                actualizarUIPomodoro();
                
                // El tiempo transcurrido se sincroniza con el servidor cada 10 segundos
                // para minimizar pérdidas de datos si la aplicación se cierra.
                if ((millisUntilFinished / 1000) % 10 == 0) {
                    enviarTiempoInvertidoAlServidor();
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                etPomodoroMinutes.setEnabled(true);
                btnStartPomodoro.setText(R.string.reiniciar);
                enviarTiempoInvertidoAlServidor();
                Toast.makeText(DetalleEventoActivity.this, R.string.tiempo_agotado, Toast.LENGTH_LONG).show();
            }
        }.start();

        isTimerRunning = true;
        btnStartPomodoro.setText(R.string.pausar);
    }

    private void pararTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        btnStartPomodoro.setText(R.string.reanudar);
        etPomodoroMinutes.setEnabled(true);
        enviarTiempoInvertidoAlServidor();
    }

    private void reiniciarTimer() {
        pararTimer();
        totalTimeInMillis = originalTimeInMillis;
        actualizarUIPomodoro();
        btnStartPomodoro.setText(R.string.iniciar);
        etPomodoroMinutes.setEnabled(true);
    }

    private void enviarTiempoInvertidoAlServidor() {
        if (eventoActual == null) return;
        
        long tiempoInvertidoSegundos = (originalTimeInMillis - totalTimeInMillis) / 1000;
        if (tiempoInvertidoSegundos <= 0) return;

        RetrofitClient.getApiService().guardarTiempoInvertido(eventoActual.getId(), tiempoInvertidoSegundos).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void actualizarUIPomodoro() {
        int minutes = (int) (totalTimeInMillis / 1000) / 60;
        int seconds = (int) (totalTimeInMillis / 1000) % 60;
        etPomodoroMinutes.setText(String.valueOf(minutes));
        tvPomodoroSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
        
        int progress = (int) (100 - (totalTimeInMillis * 100 / originalTimeInMillis));
        pbPomodoro.setProgress(progress);
    }
}
