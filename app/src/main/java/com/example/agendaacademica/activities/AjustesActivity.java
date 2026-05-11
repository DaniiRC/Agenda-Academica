package com.example.agendaacademica.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.os.LocaleListCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.agendaacademica.R;
import com.example.agendaacademica.SessionManager;
import com.example.agendaacademica.network.RetrofitClient;
import com.google.android.material.switchmaterial.SwitchMaterial;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.agendaacademica.utils.ClickGuard;

/**
 * Actividad que gestiona las preferencias globales de la aplicación.
 * Permite al usuario alternar entre temas (claro/oscuro), activar recordatorios,
 * cambiar el idioma de la interfaz, acceder a la edición de perfil y eliminar su cuenta.
 */
public class AjustesActivity extends BaseActivity {

    private SwitchMaterial switchTemaOscuro, switchRecordatorios, switchAnimations, switchHighContrast, switchColorBlind;
    private Button btnEditarPerfil, btnIdioma, btnEliminarCuenta;
    private SessionManager session;

    /**
     * Inicializa la actividad, configura la barra de herramientas y carga las preferencias del usuario.
     *
     * @param savedInstanceState Estado previo de la actividad, o {@code null} si es la primera vez.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        session = new SessionManager(this);

        configurarToolbar();
        initViews();
        cargarPreferencias();
        configurarListeners();
    }

    /**
     * Configura la barra de herramientas superior ({@link Toolbar}) y establece
     * el botón de navegación hacia atrás con el color definido en {@code R.color.text_main}.
     */
    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAjustes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.configuracion);
        }
        
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.text_main));
        }
        
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Vincula las referencias de los componentes de la interfaz con sus respectivos IDs de layout.
     */
    private void initViews() {
        switchTemaOscuro = findViewById(R.id.switchTemaOscuro);
        switchRecordatorios = findViewById(R.id.switchRecordatorios);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnIdioma = findViewById(R.id.btnIdioma);
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        switchHighContrast = findViewById(R.id.switchHighContrast);
        switchColorBlind = findViewById(R.id.switchColorBlind);
        switchAnimations = findViewById(R.id.switchAnimations);
    }

    /**
     * Lee los valores almacenados en {@link SessionManager} y los refleja en cada control
     * de la interfaz, sincronizando la vista con el estado real de las preferencias.
     */
    private void cargarPreferencias() {
        switchTemaOscuro.setChecked(session.esModoClaro());
        switchRecordatorios.setChecked(session.recordatoriosActivos());
        switchHighContrast.setChecked(session.altoContrasteActivo());
        switchColorBlind.setChecked(session.modoDaltonicoActivo());
        switchAnimations.setChecked(session.animacionesActivas());
    }

    /**
     * Registra los listeners de clic y cambio de estado para los interruptores y botones.
     * Cada control persiste su preferencia en {@link SessionManager} de forma inmediata.
     */
    private void configurarListeners() {
        switchTemaOscuro.setOnClickListener(v -> {
            if (!ClickGuard.canClick()) {
                switchTemaOscuro.setChecked(!switchTemaOscuro.isChecked());
                return;
            }
            boolean isChecked = switchTemaOscuro.isChecked();
            session.guardarModoClaro(isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        });

        switchRecordatorios.setOnCheckedChangeListener((view, isChecked) -> {
            if (!ClickGuard.canClick()) return;
            session.guardarRecordatoriosActivos(isChecked);
            int msg = isChecked ? R.string.recordatorios_activados : R.string.recordatorios_desactivados;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        btnEditarPerfil.setOnClickListener(v -> {
            if (ClickGuard.canClick()) {
                startActivity(new Intent(this, EditarPerfilActivity.class));
            }
        });

        btnIdioma.setOnClickListener(v -> {
            if (ClickGuard.canClick()) {
                mostrarDialogoIdioma();
            }
        });

        btnEliminarCuenta.setOnClickListener(v -> {
            if (ClickGuard.canClick()) {
                mostrarDialogoEliminarCuenta();
            }
        });

        switchHighContrast.setOnCheckedChangeListener((view, isChecked) -> {
            session.guardarAltoContraste(isChecked);
            aplicarAccesibilidad();
        });

        switchColorBlind.setOnCheckedChangeListener((view, isChecked) -> {
            session.guardarModoDaltonico(isChecked);
            aplicarAccesibilidad();
        });

        switchAnimations.setOnCheckedChangeListener((view, isChecked) -> {
            session.guardarAnimacionesActivas(isChecked);
        });
    }

    /**
     * Muestra un diálogo que requiere que el usuario escriba la palabra clave
     * {@code R.string.palabra_confirmacion} antes de proceder con el borrado de la cuenta.
     * Si la confirmación es correcta, delega la operación a {@link #eliminarCuentaServidor()}.
     */
    private void mostrarDialogoEliminarCuenta() {
        final String palabraConfirmacion = getString(R.string.palabra_confirmacion);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 10);

        final EditText input = new EditText(this);
        input.setHint(palabraConfirmacion);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_cuenta_perm)
                .setMessage(getString(R.string.confirmar_eliminar_cuenta) + "\n\n" + 
                           getString(R.string.escribe_confirmacion, palabraConfirmacion))
                .setView(layout)
                .setPositiveButton(R.string.eliminar, (dialog, which) -> {
                    String textoIntroducido = input.getText().toString().trim().toLowerCase();
                    if (textoIntroducido.equals(palabraConfirmacion.toLowerCase())) {
                        eliminarCuentaServidor();
                    } else {
                        Toast.makeText(this, R.string.confirmacion_incorrecta, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    /**
     * Ejecuta la petición de borrado permanente de la cuenta en el servidor.
     *
     * Muestra un {@link android.app.ProgressDialog} bloqueante durante la operación.
     * Si el servidor responde con {@code 200} o {@code 204}, cierra la sesión y redirige
     * a {@link LoginActivity} limpiando la pila de actividades. En caso de error, muestra
     * un diálogo con el mensaje devuelto por el servidor.
     */
    private void eliminarCuentaServidor() {
        Long userId = session.getUserId();
        if (userId == -1L) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.eliminando_cuenta));
        pd.setCancelable(false);
        pd.show();

        RetrofitClient.getApiService().eliminarUsuario(userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                pd.dismiss();
                if (response.isSuccessful() || response.code() == 204) {
                    Toast.makeText(AjustesActivity.this, R.string.cuenta_eliminada, Toast.LENGTH_SHORT).show();
                    session.cerrarSesion();
                    Intent intent = new Intent(AjustesActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorDetalle = "Error desconocido";
                    try {
                        if (response.errorBody() != null) errorDetalle = response.errorBody().string();
                    } catch (Exception ignored) {}

                    new AlertDialog.Builder(AjustesActivity.this)
                            .setTitle(R.string.error_eliminar_cuenta)
                            .setMessage(errorDetalle)
                            .setPositiveButton(R.string.entendido, null)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                pd.dismiss();
                Toast.makeText(AjustesActivity.this, getString(R.string.error_red) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Muestra un selector de idioma con opción única (Español, English, Français).
     *
     * Aplica el idioma seleccionado mediante
     * {@link AppCompatDelegate#setApplicationLocales(LocaleListCompat)}, lo que provoca
     * una recreación automática de la actividad. El código ISO 639-1 del idioma elegido
     * se persiste en {@link SessionManager} para restaurarlo al iniciar la aplicación.
     */
    private void mostrarDialogoIdioma() {
        String[] idiomas = {"Español", "English", "Français"};
        String[] codigos = {"es", "en", "fr"};

        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        String idiomaActual;
        if (!current.isEmpty()) {
            idiomaActual = current.get(0).getLanguage();
        } else {
            idiomaActual = java.util.Locale.getDefault().getLanguage();
        }

        int seleccionado = 0;
        for (int i = 0; i < codigos.length; i++) {
            if (idiomaActual != null && idiomaActual.startsWith(codigos[i])) {
                seleccionado = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.seleccionar_idioma)
                .setSingleChoiceItems(idiomas, seleccionado, (dialog, i) -> {
                    session.guardarIdioma(codigos[i]);
                    LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(codigos[i]);
                    AppCompatDelegate.setApplicationLocales(appLocales);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
