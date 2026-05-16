package com.example.edusync.activities;

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
import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.example.edusync.model.Usuario;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;
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
     * Inicializa la actividad, instancia el {@link SessionManager} y el servicio de red.
     *
     * @param savedInstanceState Estado previo de la actividad, o {@code null} si es la primera vez.
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
     * Infla y enlaza los componentes de la interfaz y registra los listeners de acción.
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
     * Rellena los campos de la interfaz con los datos del usuario almacenados en {@link SessionManager}.
     */
    private void cargarDatos() {
        etNombre.setText(session.obtenerNombreUsuario());
        etEmail.setText(session.obtenerEmailUsuario());

        com.example.edusync.utils.GlideUtils.cargarFotoPerfil(
                this,
                session.obtenerFotoUsuario(),
                ivFotoPerfil
        );
    }

    /**
     * Muestra un diálogo de selección de fuente de imagen (cámara o galería).
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
     * Comprueba si el permiso de cámara ha sido concedido y abre la cámara o solicita el permiso.
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
     * Lanza la cámara del dispositivo para capturar una nueva foto de perfil.
     * La imagen se guarda en un archivo temporal gestionado por {@link FileProvider}.
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
     * Crea un archivo temporal con nombre único basado en la marca de tiempo actual
     * para almacenar la imagen capturada por la cámara.
     *
     * @return El {@link File} creado en el directorio externo de la aplicación.
     * @throws Exception Si ocurre un error de I/O durante la creación del archivo.
     */
    private File createImageFile() throws Exception {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (!storageDir.exists()) storageDir.mkdirs();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * Abre la galería multimedia del dispositivo mediante un {@code Intent.ACTION_PICK}.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    /**
     * Valida el nombre introducido y persiste los cambios del perfil.
     * Si el usuario ha seleccionado una nueva foto, delega en {@link #subirFoto(String)};
     * de lo contrario, actualiza directamente el nombre en la sesión local.
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
     * Sube la nueva foto de perfil al servidor mediante una petición multipart/form-data
     * y actualiza los datos de sesión con la URL devuelta por el servidor.
     *
     * @param nombre Nombre del usuario que se actualizará junto a la foto.
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
     * Convierte una URI de contenido en un archivo JPEG comprimido y redimensionado (máx. 800×800 px)
     * listo para ser enviado al servidor.
     *
     * @param uri URI de la imagen seleccionada desde la galería o la cámara.
     * @return El {@link File} resultante con la imagen procesada.
     * @throws Exception Si ocurre un error de I/O o al decodificar la imagen con Glide.
     */
    private File uriToFile(Uri uri) throws Exception {
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
        return tempFile;
    }

    /**
     * Muestra el diálogo de cambio de contraseña en dos pasos:
     * 1. Envía un código de verificación al correo del usuario en sesión.
     * 2. Verifica el código recibido y actualiza la contraseña en el servidor.
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
        final boolean[] isPasswordVisible = {false};

        ImageView ivTogglePasswordRecuperar = dialogView.findViewById(R.id.ivTogglePasswordRecuperar);
        ivTogglePasswordRecuperar.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                etNuevaPasswordRecuperar.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePasswordRecuperar.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etNuevaPasswordRecuperar.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePasswordRecuperar.setImageResource(R.drawable.ic_visibility);
            }
            isPasswordVisible[0] = !isPasswordVisible[0];
            etNuevaPasswordRecuperar.setSelection(etNuevaPasswordRecuperar.getText().length());
        });

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
     * Verifica si una contraseña cumple los requisitos mínimos de seguridad:
     * al menos 8 caracteres, una letra mayúscula, una minúscula y un dígito.
     *
     * @param password Cadena a validar.
     * @return {@code true} si la contraseña es válida, {@code false} en caso contrario.
     */
    private boolean esPasswordSegura(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password != null && password.matches(pattern);
    }
}
