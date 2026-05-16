package com.example.edusync.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.example.edusync.adapter.AsignaturaChipAdapter;
import com.example.edusync.adapter.AsignaturaListAdapter;
import com.example.edusync.adapter.EventoAdapter;
import com.example.edusync.adapter.MiembroAdapter;
import com.example.edusync.model.Asignatura;
import com.example.edusync.model.Evento;
import com.example.edusync.model.Grupo;
import com.example.edusync.model.Usuario;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilClaseActivity extends AppCompatActivity {

    private ImageView btnVolver, ivFotoPerfil, btnSalirClase, ivProfesorFoto, btnEditarClase;
    private TextView tvNombreClase, tvProfesorClase, tvCountMiembros, tvCountTareas, tvDescripcion, tvCodigoClase, tvRoleStatus;
    private View btnCopiarCodigo;
    private RecyclerView rvEntregasClase;
    private com.google.android.material.button.MaterialButton btnFiltroEventos;
    private TextView tvTabEventos, tvTabAsignaturas;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddEvento;

    private ApiService apiService;
    private SessionManager session;
    private Long claseId, profesorId;
    private String codigoClase;
    private EventoAdapter eventoAdapter;
    private List<Evento> listaEventosCache = new ArrayList<>();
    private List<Usuario> listaMiembros = new ArrayList<>();
    private boolean isAsignaturasTab = false;
    private List<Long> asignaturasFiltradasIds = new ArrayList<>();
    private Grupo grupoActual;

    // Variables para la gestión de imágenes
    private android.net.Uri imageUri;
    private android.net.Uri photoUri;
    private ImageView ivPreviewFotoDialog;
    private androidx.activity.result.ActivityResultLauncher<Intent> galleryLauncher;
    private androidx.activity.result.ActivityResultLauncher<Intent> cameraLauncher;
    private androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_clase);

        apiService = RetrofitClient.getApiService();
        session = new SessionManager(this);

        initLaunchers();
        initViews();
        recuperarDatosIntent();

        // Configurar RecyclerView de Entregas
        rvEntregasClase.setLayoutManager(new LinearLayoutManager(this));
        // Lo inicializamos con isAdmin en false por defecto, luego se actualizará en actualizarUIConGrupo
        eventoAdapter = new EventoAdapter(new ArrayList<>(), false, (evento, isEditClick) -> {
            if (isEditClick) {
                Intent intent = new Intent(PerfilClaseActivity.this, FormularioActivity.class);
                intent.putExtra("EVENTO_ID", evento.getId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(PerfilClaseActivity.this, DetalleEventoActivity.class);
                intent.putExtra("EVENTO_ID", evento.getId());
                boolean esAdminLocal = profesorId != null && profesorId.equals(session.getUserId());
                intent.putExtra("IS_ADMIN", esAdminLocal);
                startActivity(intent);
            }
        });
        rvEntregasClase.setAdapter(eventoAdapter);

        // Cargar datos del servidor
        cargarDatosDeLaClase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar los datos cuando volvemos de crear un evento (FormularioActivity)
        if (claseId != null && claseId != -1L) {
            cargarDetallesAdicionales();
        }
    }

    private void initLaunchers() {
        galleryLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                imageUri = result.getData().getData();
                if (ivPreviewFotoDialog != null) ivPreviewFotoDialog.setImageURI(imageUri);
            }
        });

        cameraLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                imageUri = photoUri;
                if (ivPreviewFotoDialog != null) ivPreviewFotoDialog.setImageURI(imageUri);
            }
        });

        requestPermissionLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) abrirCamara();
            else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
        });
    }

    private void abrirCamara() {
        java.io.File photoFile = new java.io.File(getCacheDir(), "clase_edit_" + System.currentTimeMillis() + ".jpg");
        photoUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
        cameraLauncher.launch(intent);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void mostrarOpcionesImagen() {
        String[] options = {"Cámara", "Galería"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar Foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            abrirCamara();
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                        }
                    } else {
                        abrirGaleria();
                    }
                }).show();
    }

    private void initViews() {
        btnVolver = findViewById(R.id.btnVolver);
        btnSalirClase = findViewById(R.id.btnSalirClase);
        ivFotoPerfil = findViewById(R.id.ivIconoClase);
        ivProfesorFoto = findViewById(R.id.ivProfesorFoto);
        tvNombreClase = findViewById(R.id.tvNombreClase);
        tvProfesorClase = findViewById(R.id.tvNombreProfesor);
        tvCountMiembros = findViewById(R.id.tvCountMiembros);
        tvCountTareas = findViewById(R.id.tvCountTareas);
        tvDescripcion = findViewById(R.id.tvDescripcionClase);
        tvCodigoClase = findViewById(R.id.tvCodigoClase);
        tvRoleStatus = findViewById(R.id.tvRoleStatus);
        btnCopiarCodigo = findViewById(R.id.btnCopiarCodigo);
        btnEditarClase = findViewById(R.id.btnEditarClase);
        fabAddEvento = findViewById(R.id.fabAddEvento);
        btnFiltroEventos = findViewById(R.id.btnFiltroEventos);
        rvEntregasClase = findViewById(R.id.rvEntregasClase);

        // Inicializar vistas de pestañas para evitar NullPointer posterior
        tvTabEventos = findViewById(R.id.tvTabEventos);
        tvTabAsignaturas = findViewById(R.id.tvMiembrosTitle);

        btnVolver.setOnClickListener(v -> finish());
        btnSalirClase.setOnClickListener(v -> confirmarSalida());

        btnCopiarCodigo.setOnClickListener(v -> {
            String codigo = tvCodigoClase.getText().toString();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(getString(R.string.detalles_clase), codigo);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.codigo_copiado), Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnVerMiembros).setOnClickListener(v -> mostrarDialogoMiembros());

        // Tab pill switcher — click listeners on the TextViews directly
        tvTabEventos.setOnClickListener(v -> mostrarEventos());
        tvTabAsignaturas.setOnClickListener(v -> mostrarPestañaAsignaturas());

        btnEditarClase.setOnClickListener(v -> mostrarDialogoEditarClase());
        findViewById(R.id.btnEditarCampos).setOnClickListener(v -> mostrarDialogoEditarClase());

        fabAddEvento.setOnClickListener(v -> mostrarOpcionesFab());
    }

    private void recuperarDatosIntent() {
        claseId = getIntent().getLongExtra("CLASE_ID", -1L);
        codigoClase = getIntent().getStringExtra("CLASE_CODIGO");

        if (codigoClase != null) {
            tvCodigoClase.setText(codigoClase);
        }

        tvNombreClase.setText(getIntent().getStringExtra("CLASE_NOMBRE"));
        tvDescripcion.setText(getIntent().getStringExtra("CLASE_DESC"));

        String urlFoto = getIntent().getStringExtra("CLASE_FOTO");
        com.example.edusync.utils.GlideUtils.cargarFotoGrupo(this, urlFoto, ivFotoPerfil);

        if (tvNombreClase.getText().toString().equals(getString(R.string.cargando))) {
            tvNombreClase.setText(R.string.cargando);
        }
    }

    private void cargarDatosDeLaClase() {
        if (claseId == -1L && (codigoClase == null || codigoClase.isEmpty())) return;

        if (claseId != -1L) {
            apiService.obtenerGruposDeUsuario(session.getUserId()).enqueue(new Callback<List<Grupo>>() {
                @Override
                public void onResponse(Call<List<Grupo>> call, Response<List<Grupo>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Grupo grupo : response.body()) {
                            if (grupo.getId().equals(claseId)) {
                                actualizarUIConGrupo(grupo);
                                cargarDetallesAdicionales();
                                break;
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call<List<Grupo>> call, Throwable t) {}
            });
        } else if (codigoClase != null) {
            apiService.buscarGrupoPorCodigo(codigoClase).enqueue(new Callback<Grupo>() {
                @Override
                public void onResponse(Call<Grupo> call, Response<Grupo> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        claseId = response.body().getId();
                        actualizarUIConGrupo(response.body());
                        cargarDetallesAdicionales();
                    }
                }
                @Override
                public void onFailure(Call<Grupo> call, Throwable t) {}
            });
        }
    }

    private void actualizarUIConGrupo(Grupo grupo) {
        this.grupoActual = grupo;
        tvNombreClase.setText(grupo.getNombre());
        
        if (grupo.getDescripcion() != null && !grupo.getDescripcion().trim().isEmpty()) {
            tvDescripcion.setText(grupo.getDescripcion());
            tvDescripcion.setAlpha(1.0f);
        } else {
            tvDescripcion.setText(R.string.sin_descripcion);
            tvDescripcion.setAlpha(0.5f);
        }

        tvCodigoClase.setText(grupo.getCodigo());

        if (grupo.getProfesor() != null) {
            profesorId = grupo.getProfesor().getId();
            tvProfesorClase.setText(grupo.getProfesor().getNombre());

            boolean isAdmin = grupo.getProfesor().getId().equals(session.getUserId());
            
            if (isAdmin) {
                tvRoleStatus.setText(R.string.eres_admin_completo);
                fabAddEvento.setVisibility(View.VISIBLE);
                btnEditarClase.setVisibility(View.VISIBLE);
            } else {
                tvRoleStatus.setText(R.string.eres_alumno);
                fabAddEvento.setVisibility(View.GONE);
                btnEditarClase.setVisibility(View.GONE);
            }
            
            // Actualizar el adapter con el rol real
            if (eventoAdapter != null) {
                eventoAdapter.setAdmin(isAdmin);
            }
        }

        listaMiembros.clear();
        if (grupo.getProfesor() != null) {
            listaMiembros.add(grupo.getProfesor());
        }
        if (grupo.getAlumnos() != null) {
            for (Usuario alumno : grupo.getAlumnos()) {
                // Evitar duplicar si el profesor también aparece en la lista de alumnos
                boolean yaExiste = false;
                for (Usuario m : listaMiembros) {
                    if (m.getId().equals(alumno.getId())) {
                        yaExiste = true;
                        // Actualizamos los datos del miembro por si han cambiado (nombre, foto)
                        m.setNombre(alumno.getNombre());
                        m.setFotoUrl(alumno.getFotoUrl());
                        break;
                    }
                }
                if (!yaExiste) {
                    listaMiembros.add(alumno);
                }
            }
        }
        tvCountMiembros.setText(String.valueOf(listaMiembros.size()));

        com.example.edusync.utils.GlideUtils.cargarFotoGrupo(this, grupo.getFotoUrl(), ivFotoPerfil);

        // Cargar foto del profesor si tiene
        if (grupo.getProfesor() != null && ivProfesorFoto != null) {
            com.example.edusync.utils.GlideUtils.cargarFotoPerfil(this, grupo.getProfesor().getFotoUrl(), ivProfesorFoto);
        }
    }

    private List<Asignatura> listaAsignaturasCache = new ArrayList<>();

    private void cargarDetallesAdicionales() {
        if (claseId == -1L) return;

        apiService.obtenerEventosDeGrupo(claseId).enqueue(new Callback<List<Evento>>() {
            @Override
            public void onResponse(Call<List<Evento>> call, Response<List<Evento>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaEventosCache = response.body();
                    tvCountTareas.setText(String.valueOf(listaEventosCache.size()));
                    eventoAdapter.updateData(listaEventosCache);
                }
            }
            @Override
            public void onFailure(Call<List<Evento>> call, Throwable t) {}
        });

        apiService.obtenerAsignaturasDeGrupo(claseId).enqueue(new Callback<List<Asignatura>>() {
            @Override
            public void onResponse(Call<List<Asignatura>> call, Response<List<Asignatura>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaAsignaturasCache = response.body();
                    if (isAsignaturasTab) {
                        mostrarPestañaAsignaturas();
                    } else {
                        actualizarListaAsignaturas();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Asignatura>> call, Throwable t) {
            }
        });
    }

    private void actualizarListaAsignaturas() {
        if (listaAsignaturasCache.isEmpty()) {
            btnFiltroEventos.setVisibility(View.GONE);
            return;
        }
        
        if (!isAsignaturasTab) {
            btnFiltroEventos.setVisibility(View.VISIBLE);
            btnFiltroEventos.setOnClickListener(v -> mostrarDialogoFiltro());
        } else {
            btnFiltroEventos.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoFiltro() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filtro_asignaturas, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        android.widget.LinearLayout container = dialogView.findViewById(R.id.llContainerAsignaturas);
        
        // Lista temporal para no aplicar hasta que se de a guardar
        List<Long> tempSeleccionadas = new ArrayList<>(asignaturasFiltradasIds);

        for (Asignatura asig : listaAsignaturasCache) {
            View itemView = getLayoutInflater().inflate(R.layout.item_filtro_asignatura, container, false);
            
            TextView tvNombre = itemView.findViewById(R.id.tvNombreAsig);
            com.google.android.material.card.MaterialCardView cardColor = itemView.findViewById(R.id.cardColorAsig);
            android.widget.CheckBox cbFiltro = itemView.findViewById(R.id.cbFiltroAsig);

            tvNombre.setText(asig.getNombre());
            
            try {
                if (asig.getColor() != null && !asig.getColor().isEmpty()) {
                    cardColor.setCardBackgroundColor(android.graphics.Color.parseColor(asig.getColor()));
                }
            } catch (Exception e) {}

            cbFiltro.setChecked(tempSeleccionadas.contains(asig.getId()));

            View.OnClickListener clickListener = v -> {
                cbFiltro.setChecked(!cbFiltro.isChecked());
                if (cbFiltro.isChecked()) {
                    if (!tempSeleccionadas.contains(asig.getId())) {
                        tempSeleccionadas.add(asig.getId());
                    }
                } else {
                    tempSeleccionadas.remove(asig.getId());
                }
            };
            
            itemView.setOnClickListener(clickListener);
            
            // Si pulsan directamente en el checkbox en vez del layout entero
            cbFiltro.setOnClickListener(v -> {
                if (cbFiltro.isChecked()) {
                    if (!tempSeleccionadas.contains(asig.getId())) {
                        tempSeleccionadas.add(asig.getId());
                    }
                } else {
                    tempSeleccionadas.remove(asig.getId());
                }
            });

            container.addView(itemView);
        }

        dialogView.findViewById(R.id.btnAplicarFiltro).setOnClickListener(v -> {
            asignaturasFiltradasIds.clear();
            asignaturasFiltradasIds.addAll(tempSeleccionadas);
            aplicarFiltros();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnLimpiarFiltro).setOnClickListener(v -> {
            asignaturasFiltradasIds.clear();
            aplicarFiltros();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void aplicarFiltros() {
        if (asignaturasFiltradasIds.isEmpty()) {
            eventoAdapter.updateData(listaEventosCache);
            btnFiltroEventos.setText(R.string.filtrar);
            return;
        }

        List<Evento> filtrados = new ArrayList<>();
        for (Evento e : listaEventosCache) {
            if (e.getAsignatura() != null && asignaturasFiltradasIds.contains(e.getAsignatura().getId())) {
                filtrados.add(e);
            }
        }
        eventoAdapter.updateData(filtrados);
        btnFiltroEventos.setText(getString(R.string.filtrar) + " (" + asignaturasFiltradasIds.size() + ")");
    }



    private void mostrarEventos() {
        isAsignaturasTab = false;

        if (tvTabEventos != null) {
            tvTabEventos.setBackgroundResource(R.drawable.bg_tab_selected);
            tvTabEventos.setTextColor(getResources().getColor(R.color.on_primary));
        }
        if (tvTabAsignaturas != null) {
            tvTabAsignaturas.setBackgroundResource(R.drawable.bg_tab_unselected);
            tvTabAsignaturas.setTextColor(getResources().getColor(R.color.on_surface_variant));
        }

        rvEntregasClase.setAdapter(eventoAdapter);
        rvEntregasClase.setLayoutManager(new LinearLayoutManager(this));
        actualizarListaAsignaturas();
    }

    private void mostrarPestañaAsignaturas() {
        isAsignaturasTab = true;

        if (tvTabEventos != null) {
            tvTabEventos.setBackgroundResource(R.drawable.bg_tab_unselected);
            tvTabEventos.setTextColor(getResources().getColor(R.color.on_surface_variant));
        }
        if (tvTabAsignaturas != null) {
            tvTabAsignaturas.setBackgroundResource(R.drawable.bg_tab_selected);
            tvTabAsignaturas.setTextColor(getResources().getColor(R.color.on_primary));
        }

        // Limpiar y ocultar elementos innecesarios
        btnFiltroEventos.setVisibility(View.GONE);
        rvEntregasClase.setVisibility(View.VISIBLE);
        
        if (listaAsignaturasCache == null || listaAsignaturasCache.isEmpty()) {
            // Si está vacía, intentamos cargarla de nuevo por si acaso
            apiService.obtenerAsignaturasDeGrupo(claseId).enqueue(new Callback<List<Asignatura>>() {
                @Override
                public void onResponse(Call<List<Asignatura>> call, Response<List<Asignatura>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listaAsignaturasCache = response.body();
                        actualizarListaAsignaturasFull();
                    } else {
                        Toast.makeText(PerfilClaseActivity.this, getString(R.string.clase_sin_asignaturas), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<List<Asignatura>> call, Throwable t) {
                    Toast.makeText(PerfilClaseActivity.this, getString(R.string.error_red), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            actualizarListaAsignaturasFull();
        }
    }

    private void actualizarListaAsignaturasFull() {
        boolean isAdmin = profesorId != null && profesorId.equals(session.getUserId());
        
        AsignaturaListAdapter listAdapter = new AsignaturaListAdapter(listaAsignaturasCache, isAdmin, new AsignaturaListAdapter.OnAsignaturaActionListener() {
            @Override
            public void onAsignaturaClick(Asignatura asignatura) {
                mostrarDialogoNotasAsignatura(asignatura);
            }

            @Override
            public void onAsignaturaDelete(Asignatura asignatura) {
                new AlertDialog.Builder(PerfilClaseActivity.this)
                        .setTitle("Eliminar asignatura")
                        .setMessage("¿Estás seguro de que deseas eliminar " + asignatura.getNombre() + "?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            apiService.eliminarAsignatura(asignatura.getId()).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(PerfilClaseActivity.this, "Asignatura eliminada", Toast.LENGTH_SHORT).show();
                                        cargarDetallesAdicionales();
                                    } else {
                                        Toast.makeText(PerfilClaseActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(PerfilClaseActivity.this, getString(R.string.error_red), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        rvEntregasClase.setLayoutManager(new LinearLayoutManager(this));
        rvEntregasClase.setAdapter(listAdapter);
    }

    private void mostrarDialogoNotasAsignatura(Asignatura asignatura) {
        List<Evento> eventosAsig = new ArrayList<>();
        double sumaNotas = 0;
        int countNotas = 0;

        for (Evento e : listaEventosCache) {
            if (e.getAsignatura() != null && e.getAsignatura().getId().equals(asignatura.getId())) {
                eventosAsig.add(e);
                if (e.getNotaObtenida() != null) {
                    sumaNotas += e.getNotaObtenida();
                    countNotas++;
                }
            }
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notas_asignatura, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvNombre = dialogView.findViewById(R.id.tvNombreAsigNotas);
        TextView tvMedia = dialogView.findViewById(R.id.tvMediaAsig);
        android.widget.LinearLayout llLista = dialogView.findViewById(R.id.llListaNotas);

        tvNombre.setText(asignatura.getNombre());
        if (countNotas > 0) {
            double media = sumaNotas / countNotas;
            tvMedia.setText(String.format(java.util.Locale.getDefault(), "%.2f", media));
        } else {
            tvMedia.setText("-");
        }

        if (eventosAsig.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("No hay eventos registrados para esta asignatura.");
            tvVacio.setTextColor(getResources().getColor(R.color.text_sub));
            tvVacio.setPadding(0, 32, 0, 32);
            tvVacio.setGravity(android.view.Gravity.CENTER);
            llLista.addView(tvVacio);
        } else {
            for (Evento e : eventosAsig) {
                View itemView = getLayoutInflater().inflate(R.layout.item_nota_desglose, llLista, false);
                TextView tvTarea = itemView.findViewById(R.id.tvTareaNombre);
                TextView tvNota = itemView.findViewById(R.id.tvTareaNota);
                
                tvTarea.setText(e.getTitulo());
                if (e.getNotaObtenida() != null) {
                    tvNota.setText(String.format(java.util.Locale.getDefault(), "%.1f", e.getNotaObtenida()));
                } else {
                    tvNota.setText("N/A");
                    tvNota.setAlpha(0.5f);
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(this, DetalleEventoActivity.class);
                    intent.putExtra("EVENTO_ID", e.getId());
                    // Asumimos que si estamos viendo esto, el profesorId es el del grupo actual
                    boolean isAdminLocal = profesorId != null && profesorId.equals(session.getUserId());
                    intent.putExtra("IS_ADMIN", isAdminLocal);
                    startActivity(intent);
                });

                llLista.addView(itemView);
            }
        }

        dialogView.findViewById(R.id.btnCerrarNotas).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void mostrarDialogoMiembros() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_lista_miembros, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView rvMiembros = dialogView.findViewById(R.id.rvMiembrosDialog);
        TextView tvCount = dialogView.findViewById(R.id.tvCountMiembrosDialog);
        View btnCerrar = dialogView.findViewById(R.id.btnCerrarMiembros);

        tvCount.setText(getString(R.string.participantes_formato, listaMiembros.size()));
        rvMiembros.setLayoutManager(new LinearLayoutManager(this));
        rvMiembros.setAdapter(new MiembroAdapter(listaMiembros, profesorId));

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void confirmarSalida() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.abandonar_clase)
                .setMessage(R.string.confirmar_abandonar_clase)
                .setPositiveButton(R.string.salir, (dialog, which) -> salirDeLaClase())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void salirDeLaClase() {
        if (claseId == null || claseId == -1L) return;

        apiService.salirDeGrupo(claseId, session.getUserId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PerfilClaseActivity.this, getString(R.string.has_salido_clase), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PerfilClaseActivity.this, getString(R.string.error_salir_clase), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PerfilClaseActivity.this, getString(R.string.error_red), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarOpcionesFab() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_opciones_fab, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.cardOpcionEvento).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, FormularioActivity.class);
            intent.putExtra("GRUPO_ID", claseId);
            intent.putExtra("GRUPO_NOMBRE", tvNombreClase.getText().toString());
            startActivity(intent);
        });

        dialogView.findViewById(R.id.cardOpcionAsignatura).setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoNuevaAsignatura();
        });

        dialog.show();
    }

    private void mostrarDialogoNuevaAsignatura() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nueva_asignatura, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etNombre = dialogView.findViewById(R.id.etNombreNuevaAsignatura);

        final String[] colorSeleccionado = {"#3B36DF"};

        MaterialCardView cBlue = dialogView.findViewById(R.id.asigColorBlue);
        MaterialCardView cRed = dialogView.findViewById(R.id.asigColorRed);
        MaterialCardView cGreen = dialogView.findViewById(R.id.asigColorGreen);
        MaterialCardView cPurple = dialogView.findViewById(R.id.asigColorPurple);
        MaterialCardView cOrange = dialogView.findViewById(R.id.asigColorOrange);

        View.OnClickListener colorListener = view -> {
            cBlue.setStrokeWidth(0);
            cRed.setStrokeWidth(0);
            cGreen.setStrokeWidth(0);
            cPurple.setStrokeWidth(0);
            cOrange.setStrokeWidth(0);
            ((MaterialCardView) view).setStrokeWidth(6);
            ((MaterialCardView) view).setStrokeColor(ContextCompat.getColor(this, R.color.text_main));
            int id = view.getId();
            if (id == R.id.asigColorBlue) colorSeleccionado[0] = "#3B36DF";
            else if (id == R.id.asigColorRed) colorSeleccionado[0] = "#EF4444";
            else if (id == R.id.asigColorGreen) colorSeleccionado[0] = "#22C55E";
            else if (id == R.id.asigColorPurple) colorSeleccionado[0] = "#A855F7";
            else if (id == R.id.asigColorOrange) colorSeleccionado[0] = "#F59E0B";
        };

        cBlue.setOnClickListener(colorListener);
        cRed.setOnClickListener(colorListener);
        cGreen.setOnClickListener(colorListener);
        cPurple.setOnClickListener(colorListener);
        cOrange.setOnClickListener(colorListener);

        dialogView.findViewById(R.id.btnGuardarNuevaAsignatura).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                etNombre.setError(getString(R.string.nombre_obligatorio));
                return;
            }
            v.setEnabled(false);
            Asignatura asignatura = new Asignatura();
            asignatura.setNombre(nombre);
            asignatura.setColor(colorSeleccionado[0]);

            apiService.crearAsignatura(claseId, asignatura).enqueue(new Callback<Asignatura>() {
                @Override
                public void onResponse(Call<Asignatura> call, Response<Asignatura> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PerfilClaseActivity.this, getString(R.string.asignatura_creada), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarDetallesAdicionales();
                    } else {
                        v.setEnabled(true);
                        Toast.makeText(PerfilClaseActivity.this, getString(R.string.error_crear_asignatura), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Asignatura> call, Throwable t) {
                    v.setEnabled(true);
                    Toast.makeText(PerfilClaseActivity.this, getString(R.string.error_red), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void mostrarDialogoEditarClase() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_clase_pro, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ivPreviewFotoDialog = dialogView.findViewById(R.id.ivFotoEdit);
        if (ivFotoPerfil.getDrawable() != null) ivPreviewFotoDialog.setImageDrawable(ivFotoPerfil.getDrawable());
        
        dialogView.findViewById(R.id.fabFotoEdit).setOnClickListener(v -> mostrarOpcionesImagen());
        
        EditText etNombre = dialogView.findViewById(R.id.etNombreEdit);
        EditText etDesc = dialogView.findViewById(R.id.etDescEdit);
        etNombre.setText(tvNombreClase.getText().toString());
        etDesc.setText(tvDescripcion.getText().toString().equals(getString(R.string.sin_descripcion)) ? "" : tvDescripcion.getText().toString());

        // Gestor de Miembros dentro del diálogo
        androidx.recyclerview.widget.RecyclerView rvMiembros = dialogView.findViewById(R.id.rvMiembrosEdit);
        rvMiembros.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        // Creamos un adaptador rápido para la gestión de miembros
        MiembrosEditAdapter adapter = new MiembrosEditAdapter(listaMiembros, usuarioId -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Expulsar miembro")
                .setMessage("¿Estás seguro de que quieres expulsar a este usuario de la clase?")
                .setPositiveButton("Expulsar", (d, w) -> expulsarMiembro(usuarioId, dialog))
                .setNegativeButton("Cancelar", null)
                .show();
        });
        rvMiembros.setAdapter(adapter);

        dialogView.findViewById(R.id.btnGuardarCambios).setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevaDesc = etDesc.getText().toString().trim();
            if (nuevoNombre.isEmpty()) { etNombre.setError("Campo obligatorio"); return; }

            v.setEnabled(false);
            
            // Usamos el objeto actual y solo cambiamos lo editado
            if (grupoActual != null) {
                grupoActual.setNombre(nuevoNombre);
                grupoActual.setDescripcion(nuevaDesc);
            } else {
                grupoActual = new Grupo();
                grupoActual.setId(claseId);
                grupoActual.setNombre(nuevoNombre);
                grupoActual.setDescripcion(nuevaDesc);
                if (profesorId != null) {
                    Usuario prof = new Usuario(); prof.setId(profesorId);
                    grupoActual.setProfesor(prof);
                }
            }

            apiService.actualizarGrupo(claseId, grupoActual).enqueue(new Callback<Grupo>() {
                @Override
                public void onResponse(Call<Grupo> call, Response<Grupo> response) {
                    if (response.isSuccessful()) {
                        if (imageUri != null) subirFotoYFinalizar(response.body(), dialog);
                        else {
                            actualizarUIConGrupo(response.body());
                            dialog.dismiss();
                        }
                    } else {
                        v.setEnabled(true);
                        Toast.makeText(PerfilClaseActivity.this, "Error al guardar cambios", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Grupo> call, Throwable t) {
                    v.setEnabled(true);
                    Toast.makeText(PerfilClaseActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void expulsarMiembro(Long targetUsuarioId, androidx.appcompat.app.AlertDialog configDialog) {
        apiService.salirDeGrupo(claseId, targetUsuarioId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PerfilClaseActivity.this, "Miembro expulsado", Toast.LENGTH_SHORT).show();
                    // Refrescar datos de la clase para ver la lista actualizada
                    cargarDatosDeLaClase();
                    configDialog.dismiss(); // Cerramos para refrescar todo
                } else {
                    Toast.makeText(PerfilClaseActivity.this, "No se pudo expulsar al miembro", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PerfilClaseActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Adaptador interno para la gestión de miembros en el diálogo
    private class MiembrosEditAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<MiembrosEditAdapter.ViewHolder> {
        private List<Usuario> miembros;
        private java.util.function.Consumer<Long> onKickClick;

        public MiembrosEditAdapter(List<Usuario> miembros, java.util.function.Consumer<Long> onKickClick) {
            this.miembros = miembros;
            this.onKickClick = onKickClick;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_miembro, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Usuario u = miembros.get(position);
            holder.tvNombre.setText(u.getNombre());
            holder.tvRol.setText(u.getEmail());
            
            // No permitirse expulsarse a uno mismo (al admin)
            if (u.getId().equals(session.getUserId())) {
                holder.btnKick.setVisibility(View.GONE);
                holder.tvRol.setText("Tú (Administrador)");
            } else {
                holder.btnKick.setVisibility(View.VISIBLE);
                holder.btnKick.setImageResource(android.R.drawable.ic_menu_delete);
                holder.btnKick.setOnClickListener(v -> onKickClick.accept(u.getId()));
            }
        }

        @Override public int getItemCount() { return miembros.size(); }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView tvNombre, tvRol;
            ImageView btnKick;
            public ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tvNombreMiembro);
                tvRol = itemView.findViewById(R.id.tvRolMiembro);
                btnKick = itemView.findViewById(R.id.ivAdminStatus); // Reutilizamos este ID para el botón de expulsar
            }
        }
    }

    private void subirFotoYFinalizar(Grupo grupo, androidx.appcompat.app.AlertDialog dialog) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(imageUri);
            java.io.File tempFile = java.io.File.createTempFile("upload_edit", ".jpg", getCacheDir());
            try (java.io.FileOutputStream os = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            }
            
            okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), tempFile);
            okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("foto", tempFile.getName(), reqFile);

            apiService.subirFotoGrupo(grupo.getId(), body).enqueue(new Callback<Grupo>() {
                @Override
                public void onResponse(Call<Grupo> call, Response<Grupo> response) {
                    imageUri = null;
                    if (response.isSuccessful() && response.body() != null) {
                        actualizarUIConGrupo(response.body());
                    }
                    Toast.makeText(PerfilClaseActivity.this, "Clase y foto actualizadas", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onFailure(Call<Grupo> call, Throwable t) {
                    Toast.makeText(PerfilClaseActivity.this, "Datos guardados, pero error al subir foto", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            dialog.dismiss();
        }
    }
}
