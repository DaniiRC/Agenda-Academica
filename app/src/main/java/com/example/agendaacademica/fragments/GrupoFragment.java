package com.example.agendaacademica.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.activities.AjustesActivity;
import com.example.agendaacademica.model.Asignatura;
import com.example.agendaacademica.model.Grupo;
import com.example.agendaacademica.network.ApiService;
import com.example.agendaacademica.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento que permite buscar un grupo por código y unirse a él, o crear uno nuevo.
 * Gestiona la carga de imagen de la clase desde cámara o galería y el alta de asignaturas iniciales.
 */
public class GrupoFragment extends Fragment {

    private EditText etClassCode;
    private MaterialCardView cardGrupo;
    private TextView tvNombre, tvMiembrosGrupo;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    private Grupo grupoEncontrado;
    private SessionManager session;
    private ApiService apiService;
    private String colorSeleccionado = "#4F46E5"; // Color por defecto (índigo) asignado a nuevas asignaturas
    private List<String> asignaturasNuevas = new ArrayList<>();

    private Uri imageUri;
    private Uri photoUri;
    private ImageView ivPreviewFoto;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());
        apiService = RetrofitClient.getApiService();
        initLaunchers();
    }

    private void initLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                imageUri = result.getData().getData();
                if (ivPreviewFoto != null) ivPreviewFoto.setImageURI(imageUri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                imageUri = photoUri;
                if (ivPreviewFoto != null) ivPreviewFoto.setImageURI(imageUri);
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) abrirCamara();
            else Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grupo, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        etClassCode = view.findViewById(R.id.etClassCode);
        cardGrupo = view.findViewById(R.id.cardGroupFound);
        tvNombre = view.findViewById(R.id.tvNombreGrupo);
        tvMiembrosGrupo = view.findViewById(R.id.tvMiembrosGrupo);
        progressBar = view.findViewById(R.id.progressBarGrupo);
        swipeRefresh = view.findViewById(R.id.swipeRefreshGrupo);

        view.findViewById(R.id.btnUnirseClase).setOnClickListener(v -> {
            String codigo = etClassCode.getText().toString().trim();
            if (!codigo.isEmpty()) buscarGrupoBackend(codigo);
        });

        cardGrupo.setOnClickListener(v -> {
            if (grupoEncontrado != null) unirseAutomaticamente(grupoEncontrado.getCodigo(), null, grupoEncontrado.getId());
        });

        view.findViewById(R.id.btnCrearClase).setOnClickListener(v -> mostrarDialogoCrearClase());
        
        view.findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AjustesActivity.class));
        });

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                etClassCode.setText("");
                cardGrupo.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            });
        }
    }

    private void buscarGrupoBackend(String codigo) {
        setLoading(true);
        apiService.buscarGrupoPorCodigo(codigo).enqueue(new Callback<Grupo>() {
            @Override
            public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    grupoEncontrado = response.body();
                    tvNombre.setText(grupoEncontrado.getNombre());
                    int miembros = grupoEncontrado.getAlumnos() != null ? grupoEncontrado.getAlumnos().size() : 0;
                    tvMiembrosGrupo.setText(String.format(Locale.getDefault(), "Miembros: %d", miembros));
                    cardGrupo.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getContext(), "Grupo no encontrado", Toast.LENGTH_SHORT).show();
                    cardGrupo.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCrearClase() {
        if (getContext() == null) return;
        
        imageUri = null;
        asignaturasNuevas.clear();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_crear_asignatura, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ivPreviewFoto = dialogView.findViewById(R.id.ivFotoClaseNueva);
        FloatingActionButton fabFoto = dialogView.findViewById(R.id.fabSeleccionarFoto);
        EditText etNombre = dialogView.findViewById(R.id.etNombreAsignatura);
        EditText etDesc = dialogView.findViewById(R.id.etDescripcionAsignatura);
        EditText etNuevaAsignatura = dialogView.findViewById(R.id.etNuevaAsignatura);
        MaterialButton btnAddAsignatura = dialogView.findViewById(R.id.btnAddAsignaturaChip);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupAsignaturas);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarAsignatura);

        if (fabFoto != null) fabFoto.setOnClickListener(v -> mostrarOpcionesImagen());
        
        btnAddAsignatura.setOnClickListener(v -> {
            String nombreAsignatura = etNuevaAsignatura.getText().toString().trim();
            if (!nombreAsignatura.isEmpty()) {
                asignaturasNuevas.add(nombreAsignatura);
                Chip chip = new Chip(requireContext());
                chip.setText(nombreAsignatura);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v1 -> {
                    asignaturasNuevas.remove(nombreAsignatura);
                    chipGroup.removeView(chip);
                });
                chipGroup.addView(chip);
                etNuevaAsignatura.setText("");
            }
        });

        btnGuardar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                etNombre.setError(getString(R.string.nombre_obligatorio));
                return;
            }
            btnGuardar.setEnabled(false);
            crearGrupoBackend(nombre, etDesc.getText().toString().trim(), colorSeleccionado, dialog);
        });

        dialog.show();
    }

    private void mostrarOpcionesImagen() {
        String[] options = {"Cámara", "Galería"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar Foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) comprobarPermisosYAbrirCamara();
                    else abrirGaleria();
                }).show();
    }

    private void comprobarPermisosYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        File photoFile = new File(requireContext().getCacheDir(), "clase_" + System.currentTimeMillis() + ".jpg");
        photoUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
        cameraLauncher.launch(intent);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void crearGrupoBackend(String nombre, String descripcion, String color, AlertDialog dialog) {
        apiService.crearGrupo(nombre, descripcion, color, session.getUserId()).enqueue(new Callback<Grupo>() {
            @Override
            public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subirFotoYUnir(response.body(), dialog);
                } else {
                    Toast.makeText(getContext(), "Error al crear la clase", Toast.LENGTH_SHORT).show();
                    View btn = dialog.findViewById(R.id.btnGuardarAsignatura);
                    if (btn != null) btn.setEnabled(true);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
                View btn = dialog.findViewById(R.id.btnGuardarAsignatura);
                if (btn != null) btn.setEnabled(true);
            }
        });
    }

    private void subirFotoYUnir(Grupo grupo, AlertDialog dialog) {
        if (imageUri != null) {
            try {
                File file = uriToFile(imageUri);
                String mimeType = requireContext().getContentResolver().getType(imageUri);
                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }
                RequestBody reqFile = RequestBody.create(MediaType.parse(mimeType), file);
                MultipartBody.Part body = MultipartBody.Part.createFormData("foto", file.getName(), reqFile);

                apiService.subirFotoGrupo(grupo.getId(), body).enqueue(new Callback<Grupo>() {
                    @Override
                    public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                        if (!response.isSuccessful()) {
                            try {
                                android.util.Log.e("UploadFoto", "Error: " + response.errorBody().string());
                            } catch (Exception e) {}
                        }
                        unirseAutomaticamente(grupo.getCodigo(), dialog, grupo.getId());
                    }
                    @Override
                    public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                        android.util.Log.e("UploadFoto", "Failure: " + t.getMessage());
                        unirseAutomaticamente(grupo.getCodigo(), dialog, grupo.getId());
                    }
                });
            } catch (Exception e) { 
                e.printStackTrace();
                android.util.Log.e("UploadFoto", "Exception: " + e.getMessage());
                unirseAutomaticamente(grupo.getCodigo(), dialog, grupo.getId()); 
            }
        } else {
            unirseAutomaticamente(grupo.getCodigo(), dialog, grupo.getId());
        }
    }

    private File uriToFile(Uri uri) throws Exception {
        InputStream is = requireContext().getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload", ".jpg", requireContext().getCacheDir());
        try (FileOutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
        }
        return tempFile;
    }

    private void unirseAutomaticamente(String codigo, AlertDialog dialog, Long grupoId) {
        apiService.unirseAGrupo(codigo, session.getUserId()).enqueue(new Callback<Grupo>() {
            @Override
            public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                if (isAdded()) {
                    if (response.isSuccessful()) {
                        // Si el diálogo es null, el usuario se unió desde búsqueda (no desde crear clase)
                        boolean esUnion = (dialog == null);
                        crearAsignaturasParaGrupo(grupoId, dialog, esUnion);
                    } else {
                        Toast.makeText(getContext(), "No se pudo unir al grupo", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error de conexión al unirse", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void crearAsignaturasParaGrupo(Long grupoId, AlertDialog dialog, boolean esUnion) {
        if (asignaturasNuevas.isEmpty()) {
            finalizarOperacion(dialog, esUnion);
            return;
        }

        // Contador compartido por los callbacks asíncronos: finaliza la operación cuando todas las peticiones concluyen.
        final int totalAsignaturas = asignaturasNuevas.size();
        final int[] completadas = {0};

        for (String nombreAsignatura : asignaturasNuevas) {
            Asignatura asignatura = new Asignatura();
            asignatura.setNombre(nombreAsignatura);
            asignatura.setColor(colorSeleccionado);
            
            apiService.crearAsignatura(grupoId, asignatura).enqueue(new Callback<Asignatura>() {
                @Override
                public void onResponse(Call<Asignatura> call, Response<Asignatura> response) {
                    completadas[0]++;
                    if (completadas[0] == totalAsignaturas) {
                        finalizarOperacion(dialog, esUnion);
                    }
                }

                @Override
                public void onFailure(Call<Asignatura> call, Throwable t) {
                    completadas[0]++;
                    if (completadas[0] == totalAsignaturas) {
                        finalizarOperacion(dialog, esUnion);
                    }
                }
            });
        }
    }

    private void finalizarOperacion(AlertDialog dialog, boolean esUnion) {
        irAMisClases();
        String mensaje = esUnion ? "¡Te has unido a la clase!" : "¡Clase creada!";
        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        if (dialog != null) dialog.dismiss();
    }

    private void irAMisClases() {
        if (getActivity() != null) {
            BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setSelectedItemId(R.id.nav_mis_clases);
        }
    }

    /**
     * Muestra u oculta el indicador de carga mientras se realiza una operación de red.
     */
    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
