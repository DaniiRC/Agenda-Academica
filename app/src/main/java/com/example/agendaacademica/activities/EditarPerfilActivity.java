package com.example.agendaacademica.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.model.Usuario;
import com.example.agendaacademica.network.ApiService;
import com.example.agendaacademica.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad que permite al usuario gestionar su información de perfil.
 * Incluye funcionalidades para actualizar el nombre, cambiar la foto de perfil
 * (mediante cámara o galería) y modificar la contraseña de acceso.
 */
public class EditarPerfilActivity extends AppCompatActivity {

    private ImageView ivFotoPerfil;
    private EditText etNombre, etEmail;
    private MaterialButton btnGuardar, btnCambiarPass;
    private FloatingActionButton fabCambiarFoto;
    
    private SessionManager session;
    private ApiService apiService;
    private Uri uriFotoSeleccionada;
    private Uri photoUri;

    /**
     * Inicializa la actividad y los servicios de red y sesión.
     * 
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        session = new SessionManager(this);
        apiService = RetrofitClient.getApiService();

        initViews();
        cargarDatos();
    }

    /**
     * Configura las vistas y asigna los listeners a los botones.
     */
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarEditarPerfil);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ivFotoPerfil = findViewById(R.id.ivFotoPerfilEditar);
        etNombre = findViewById(R.id.etNombreEditar);
        etEmail = findViewById(R.id.etEmailEditar);
        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnCambiarPass = findViewById(R.id.btnCambiarPassEditar);
        fabCambiarFoto = findViewById(R.id.fabCambiarFoto);

        fabCambiarFoto.setOnClickListener(v -> mostrarOpcionesImagen());
        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCambiarPass.setOnClickListener(v -> mostrarDialogoCambioPassword());
    }

    /**
     * Carga la información actual del usuario desde la sesión.
     */
    private void cargarDatos() {
        etNombre.setText(session.obtenerNombreUsuario());
        etEmail.setText(session.obtenerEmailUsuario());

        com.example.agendaacademica.utils.GlideUtils.cargarFotoPerfil(
                this,
                session.obtenerFotoUsuario(),
                ivFotoPerfil
        );
    }

    /**
     * Muestra un diálogo para que el usuario elija entre capturar una foto o seleccionarla de la galería.
     */
    private void mostrarOpcionesImagen() {
        String[] opciones = {"Cámara", "Galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                comprobarPermisosYAbrirCamara();
            } else {
                abrirGaleria();
            }
        });
        builder.show();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    abrirCamara();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    /**
     * Verifica los permisos de cámara y solicita acceso si no han sido concedidos.
     */
    private void comprobarPermisosYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    uriFotoSeleccionada = photoUri;
                    Glide.with(this).load(uriFotoSeleccionada).circleCrop().into(ivFotoPerfil);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uriFotoSeleccionada = result.getData().getData();
                    Glide.with(this).load(uriFotoSeleccionada).circleCrop().into(ivFotoPerfil);
                }
            }
    );

    /**
     * Lanza la intención de la cámara para capturar una nueva foto de perfil.
     */
    private void abrirCamara() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraLauncher.launch(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al crear el archivo de imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Crea un archivo temporal para almacenar la imagen capturada por la cámara.
     * 
     * @return El archivo creado.
     * @throws Exception Si ocurre un error en la creación del archivo.
     */
    private File createImageFile() throws Exception {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (!storageDir.exists()) storageDir.mkdirs();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * Abre la galería del dispositivo para seleccionar una imagen.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    /**
     * Valida y persiste los cambios realizados en el perfil.
     */
    private void guardarCambios() {
        String nuevoNombre = etNombre.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uriFotoSeleccionada != null) {
            subirFoto(nuevoNombre);
        } else {
            session.guardarUsuario(session.getUserId(), nuevoNombre, session.obtenerEmailUsuario(), session.obtenerFotoUsuario(), session.obtenerRol());
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Sube la nueva foto de perfil al servidor mediante una petición multipart.
     * 
     * @param nombre Nombre del usuario para actualizar simultáneamente.
     */
    private void subirFoto(String nombre) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = uriToFile(uriFotoSeleccionada);
                
                runOnUiThread(() -> {
                    RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                    MultipartBody.Part body = MultipartBody.Part.createFormData("foto", file.getName(), requestFile);

                    apiService.subirFotoUsuario(session.getUserId(), body).enqueue(new Callback<Usuario>() {
                        @Override
                        public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Usuario u = response.body();
                                String urlRefrescada = u.getFotoUrl() + "?t=" + System.currentTimeMillis();
                                session.guardarUsuario(u.getId(), nombre, u.getEmail(), urlRefrescada, u.getRol());
                                
                                Toast.makeText(EditarPerfilActivity.this, "Perfil y foto actualizados", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(EditarPerfilActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Usuario> call, Throwable t) {
                            Toast.makeText(EditarPerfilActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditarPerfilActivity.this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Convierte una URI de contenido en un archivo físico, redimensionándolo y comprimiéndolo.
     * 
     * @param uri URI de la imagen seleccionada.
     * @return El archivo resultante listo para subir.
     * @throws Exception Si ocurre un error en el procesamiento.
     */
    private File uriToFile(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        android.graphics.Bitmap bitmap = Glide.with(this)
                .asBitmap()
                .load(uri)
                .override(800, 800)
                .submit()
                .get();

        File tempFile = File.createTempFile("upload_perfil", ".jpg", getCacheDir());
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream);
            outputStream.flush();
        }
        inputStream.close();
        return tempFile;
    }

    /**
     * Muestra un diálogo especializado para el cambio de contraseña.
     * Reutiliza la lógica de recuperación por código para garantizar la identidad del usuario.
     */
    private void mostrarDialogoCambioPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_recuperar_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etEmailRecuperar = dialogView.findViewById(R.id.etEmailRecuperar);
        EditText etCodigoRecuperar = dialogView.findViewById(R.id.etCodigoRecuperar);
        EditText etNuevaPasswordRecuperar = dialogView.findViewById(R.id.etNuevaPasswordRecuperar);
        MaterialButton btnAccionRecuperar = dialogView.findViewById(R.id.btnEnviarRecuperar);
        android.widget.ProgressBar pbRecuperar = dialogView.findViewById(R.id.pbRecuperar);
        android.widget.TextView tvDesc = dialogView.findViewById(R.id.tvDescRecuperar);
        android.widget.FrameLayout layoutEmail = dialogView.findViewById(R.id.layoutEmailRecuperar);
        android.widget.LinearLayout layoutCodigo = dialogView.findViewById(R.id.layoutCodigoVerificacion);

        String userEmail = session.obtenerEmailUsuario();
        etEmailRecuperar.setText(userEmail);
        etEmailRecuperar.setEnabled(false);

        final boolean[] esPasoCodigo = {false};

        btnAccionRecuperar.setOnClickListener(v -> {
            if (!esPasoCodigo[0]) {
                pbRecuperar.setVisibility(android.view.View.VISIBLE);
                btnAccionRecuperar.setEnabled(false);
                btnAccionRecuperar.setTextColor(android.graphics.Color.TRANSPARENT);

                RetrofitClient.getApiService().enviarCodigo(userEmail).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        pbRecuperar.setVisibility(android.view.View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        if (response.isSuccessful()) {
                            esPasoCodigo[0] = true;
                            layoutEmail.setVisibility(android.view.View.GONE);
                            layoutCodigo.setVisibility(android.view.View.VISIBLE);
                            tvDesc.setText("Introduce el código enviado a tu correo.");
                            btnAccionRecuperar.setText("Verificar y Cambiar");
                        } else {
                            Toast.makeText(EditarPerfilActivity.this, "Error al enviar código", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        pbRecuperar.setVisibility(android.view.View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        Toast.makeText(EditarPerfilActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                String codigo = etCodigoRecuperar.getText().toString().trim();
                String nuevaPass = etNuevaPasswordRecuperar.getText().toString().trim();

                if (codigo.length() < 6) {
                    Toast.makeText(this, "Código incompleto", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!esPasswordSegura(nuevaPass)) {
                    Toast.makeText(this, getString(R.string.password_requisitos), Toast.LENGTH_LONG).show();
                    return;
                }

                pbRecuperar.setVisibility(android.view.View.VISIBLE);
                btnAccionRecuperar.setEnabled(false);
                btnAccionRecuperar.setTextColor(android.graphics.Color.TRANSPARENT);

                RetrofitClient.getApiService().verificarYCambiar(userEmail, codigo, nuevaPass).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        pbRecuperar.setVisibility(android.view.View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        if (response.isSuccessful()) {
                            Toast.makeText(EditarPerfilActivity.this, "Contraseña actualizada", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(EditarPerfilActivity.this, "Código incorrecto", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        pbRecuperar.setVisibility(android.view.View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        Toast.makeText(EditarPerfilActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }
    /**
     * Verifica si una contraseña cumple con los requisitos mínimos de seguridad.
     */
    private boolean esPasswordSegura(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password != null && password.matches(pattern);
    }
}