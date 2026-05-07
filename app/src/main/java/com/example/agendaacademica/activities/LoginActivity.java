package com.example.agendaacademica.activities;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.model.LoginResponse;
import com.example.agendaacademica.model.Usuario;
import com.example.agendaacademica.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad encargada de la autenticación de usuarios.
 * Gestiona los procesos de inicio de sesión, registro de nuevos usuarios
 * y recuperación de contraseña mediante envío de códigos de verificación.
 */
public class LoginActivity extends BaseActivity {

    private View inputLayoutNombre, inputLayoutEmail, inputLayoutPassword;
    private EditText etNombre, etEmail, etPassword;
    private MaterialButton btnAccion;
    private ProgressBar progressBar;
    private TextView tvTitulo, tvSubtitulo, tvCambiarModo, tvLabelNuevo, tvOlvidePassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;
    private boolean esModoRegistro = false;

    /**
     * Inicializa la actividad y verifica si ya existe una sesión activa.
     * 
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new SessionManager(this);

        if (session.estaLogueado()) {
            irAlMain();
            super.onCreate(savedInstanceState);
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
    }

    /**
     * Inicializa las vistas de la interfaz y configura los listeners de eventos.
     */
    private void initViews() {
        inputLayoutNombre = findViewById(R.id.inputLayoutNombre);
        inputLayoutEmail = findViewById(R.id.inputLayoutEmail);
        inputLayoutPassword = findViewById(R.id.inputLayoutPassword);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnAccion = findViewById(R.id.btnAccion);
        progressBar = findViewById(R.id.progressBar);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvCambiarModo = findViewById(R.id.tvCambiarModo);
        tvLabelNuevo = findViewById(R.id.tvLabelNuevo);
        tvOlvidePassword = findViewById(R.id.tvOlvidePassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        LinearLayout loginCard = findViewById(R.id.loginCard);
        if (loginCard != null) {
            loginCard.setLayoutTransition(new LayoutTransition());
        }

        tvCambiarModo.setOnClickListener(v -> toggleModo());
        btnAccion.setOnClickListener(v -> validarYEnviar());
        tvOlvidePassword.setOnClickListener(v -> mostrarDialogoRecuperacion());
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    /**
     * Cambia la visibilidad de la contraseña entre texto plano y caracteres ocultos.
     */
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    /**
     * Muestra el diálogo para la recuperación de contraseña.
     * Gestiona el flujo de envío de código y cambio de contraseña mediante un proceso de dos pasos.
     */
    private void mostrarDialogoRecuperacion() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_recuperar_password, null);
        builder.setView(dialogView);

        EditText etEmailRecuperar = dialogView.findViewById(R.id.etEmailRecuperar);
        EditText etCodigoRecuperar = dialogView.findViewById(R.id.etCodigoRecuperar);
        EditText etNuevaPasswordRecuperar = dialogView.findViewById(R.id.etNuevaPasswordRecuperar);
        ImageView ivToggleRecuperar = dialogView.findViewById(R.id.ivTogglePasswordRecuperar);
        final boolean[] isPasswordVisibleRecuperar = {false};
        
        ivToggleRecuperar.setOnClickListener(v2 -> {
            if (isPasswordVisibleRecuperar[0]) {
                etNuevaPasswordRecuperar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleRecuperar.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etNuevaPasswordRecuperar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleRecuperar.setImageResource(R.drawable.ic_visibility);
            }
            isPasswordVisibleRecuperar[0] = !isPasswordVisibleRecuperar[0];
            etNuevaPasswordRecuperar.setSelection(etNuevaPasswordRecuperar.getText().length());
        });

        View layoutEmail = dialogView.findViewById(R.id.layoutEmailRecuperar);
        View layoutCodigo = dialogView.findViewById(R.id.layoutCodigoVerificacion);
        TextView tvDesc = dialogView.findViewById(R.id.tvDescRecuperar);
        MaterialButton btnAccionRecuperar = dialogView.findViewById(R.id.btnEnviarRecuperar);
        ProgressBar pbRecuperar = dialogView.findViewById(R.id.pbRecuperar);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final boolean[] esPasoCodigo = {false};

        btnAccionRecuperar.setOnClickListener(v -> {
            String email = etEmailRecuperar.getText().toString().trim();

            if (!esPasoCodigo[0]) {
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Introduce un email válido", Toast.LENGTH_SHORT).show();
                    return;
                }

                pbRecuperar.setVisibility(View.VISIBLE);
                btnAccionRecuperar.setEnabled(false);
                btnAccionRecuperar.setTextColor(android.graphics.Color.TRANSPARENT);

                RetrofitClient.getApiService().enviarCodigo(email).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        pbRecuperar.setVisibility(View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        if (response.isSuccessful()) {
                            esPasoCodigo[0] = true;
                            layoutEmail.setVisibility(View.GONE);
                            layoutCodigo.setVisibility(View.VISIBLE);
                            tvDesc.setText("Introduce el código de 6 dígitos enviado a tu correo.");
                            btnAccionRecuperar.setText("Verificar y Cambiar");
                            Toast.makeText(LoginActivity.this, "Código enviado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "El email no está registrado", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        pbRecuperar.setVisibility(View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                String codigo = etCodigoRecuperar.getText().toString().trim();
                String nuevaPass = etNuevaPasswordRecuperar.getText().toString().trim();

                if (codigo.length() < 6) {
                    Toast.makeText(this, "Introduce el código de 6 dígitos", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!esPasswordSegura(nuevaPass)) {
                    Toast.makeText(this, getString(R.string.password_requisitos), Toast.LENGTH_LONG).show();
                    return;
                }

                pbRecuperar.setVisibility(View.VISIBLE);
                btnAccionRecuperar.setEnabled(false);
                btnAccionRecuperar.setTextColor(android.graphics.Color.TRANSPARENT);

                RetrofitClient.getApiService().verificarYCambiar(email, codigo, nuevaPass).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        pbRecuperar.setVisibility(View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        if (response.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(LoginActivity.this, "Código incorrecto", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        pbRecuperar.setVisibility(View.GONE);
                        btnAccionRecuperar.setEnabled(true);
                        btnAccionRecuperar.setTextColor(android.graphics.Color.WHITE);
                        Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    /**
     * Alterna la interfaz entre el modo de Inicio de Sesión y el modo de Registro.
     */
    private void toggleModo() {
        esModoRegistro = !esModoRegistro;

        if (esModoRegistro) {
            tvTitulo.setText("Crear Cuenta");
            tvSubtitulo.setText("Únete a nuestra comunidad académica.");
            btnAccion.setText("Registrarse");
            tvLabelNuevo.setVisibility(View.GONE);
            tvCambiarModo.setText("¿Ya tienes cuenta? Inicia sesión");
            tvCambiarModo.setTextColor(getResources().getColor(R.color.login_outline));
            tvOlvidePassword.setVisibility(View.GONE);
            inputLayoutNombre.setVisibility(View.VISIBLE);
        } else {
            tvTitulo.setText("Bienvenido de nuevo");
            tvSubtitulo.setText("Continúa tu trayectoria académica.");
            btnAccion.setText("Iniciar Sesión");
            tvLabelNuevo.setVisibility(View.VISIBLE);
            tvCambiarModo.setText(" Crea una cuenta");
            tvCambiarModo.setTextColor(getResources().getColor(R.color.login_primary));
            tvOlvidePassword.setVisibility(View.VISIBLE);
            inputLayoutNombre.setVisibility(View.GONE);
        }
    }

    /**
     * Valida los campos de entrada del usuario y procede con la acción de login o registro.
     */
    private void validarYEnviar() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean esValido = true;

        if (email.isEmpty()) {
            showError("Introduce tu email");
            esValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Formato de email no válido");
            esValido = false;
        } else if (password.isEmpty()) {
            showError("Introduce tu contraseña");
            esValido = false;
        } else if (esModoRegistro && !esPasswordSegura(password)) {
            showError(getString(R.string.password_requisitos));
            esValido = false;
        } else if (!esModoRegistro && password.length() < 4) {
            showError("La contraseña es demasiado corta");
            esValido = false;
        }

        if (esValido && esModoRegistro) {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                showError("El nombre es obligatorio");
                esValido = false;
            }
        }

        if (esValido) {
            if (esModoRegistro) {
                registrarUsuario(etNombre.getText().toString().trim(), email, password);
            } else {
                login(email, password);
            }
        }
    }

    /**
     * Muestra un mensaje de error mediante un Toast.
     * 
     * @param message Mensaje a mostrar.
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Verifica si una contraseña cumple con los requisitos mínimos de seguridad:
     * - Al menos 8 caracteres.
     * - Al menos una letra mayúscula.
     * - Al menos una letra minúscula.
     * - Al menos un número.
     *
     * @param password Contraseña a validar.
     * @return Verdadero si cumple los requisitos, falso en caso contrario.
     */
    private boolean esPasswordSegura(String password) {
        // Regex: Al menos 8 caracteres, una mayúscula, una minúscula y un número
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password != null && password.matches(pattern);
    }

    /**
     * Realiza la petición de registro de un nuevo usuario al servidor.
     * 
     * @param nombre   Nombre del usuario.
     * @param email    Correo electrónico.
     * @param password Contraseña elegida.
     */
    private void registrarUsuario(String nombre, String email, String password) {
        setLoading(true);
        Usuario nuevoUsuario = new Usuario(nombre, email, password);

        RetrofitClient.getApiService().registrarUsuario(nuevoUsuario).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse lr = response.body();
                    session.guardarToken(lr.getToken());
                    session.guardarUsuario(lr.getId(), lr.getNombre(), lr.getEmail(), lr.getFotoUrl(), lr.getRol());
                    irAlMain();
                } else {
                    showError("Este email ya está en uso");
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                setLoading(false);
                showError("Error de conexión");
            }
        });
    }

    /**
     * Realiza la petición de inicio de sesión al servidor.
     * 
     * @param email    Correo electrónico.
     * @param password Contraseña.
     */
    private void login(String email, String password) {
        setLoading(true);
        Usuario credenciales = new Usuario(email, password);

        RetrofitClient.getApiService().login(credenciales).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse lr = response.body();
                    session.guardarToken(lr.getToken());
                    session.guardarUsuario(lr.getId(), lr.getNombre(), lr.getEmail(), lr.getFotoUrl(), lr.getRol());
                    irAlMain();
                } else {
                    showError("Credenciales incorrectas");
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                setLoading(false);
                showError("Error de conexión");
            }
        });
    }

    /**
     * Controla el estado de carga de la interfaz, bloqueando botones y mostrando el indicador visual.
     * 
     * @param isLoading Verdadero para activar el modo carga, falso para desactivarlo.
     */
    private void setLoading(boolean isLoading) {
        btnAccion.setEnabled(!isLoading);
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnAccion.setTextColor(isLoading ? android.graphics.Color.TRANSPARENT : android.graphics.Color.WHITE);
            if (isLoading) {
                btnAccion.setIcon(null);
            } else {
                btnAccion.setIconResource(android.R.drawable.ic_menu_send);
            }
        }
    }

    /**
     * Redirige al usuario según su rol (ADMIN o USER).
     */
    private void redirigirSegunRol() {
        if (session.esAdmin()) {
            startActivity(new Intent(this, AdminActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    /**
     * Navega hacia la actividad principal y finaliza la actividad actual.
     */
    private void irAlMain() {
        redirigirSegunRol();
    }
}
