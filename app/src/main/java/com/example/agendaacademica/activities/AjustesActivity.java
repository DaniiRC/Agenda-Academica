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
     * @param savedInstanceState Estado previo de la actividad.
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
     * Configura la barra de herramientas superior y el botón de navegación hacia atrás.
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
     * Inicializa las referencias a los componentes de la interfaz.
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
     * Sincroniza los controles de la interfaz con los valores guardados en la sesión.
     */
    private void cargarPreferencias() {
        switchTemaOscuro.setChecked(session.esModoClaro());
        switchRecordatorios.setChecked(session.recordatoriosActivos());
        switchHighContrast.setChecked(session.altoContrasteActivo());
        switchColorBlind.setChecked(session.modoDaltonicoActivo());
        switchAnimations.setChecked(session.animacionesActivas());
    }

    /**
     * Configura los eventos de clic y cambio de estado para todos los controles.
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
            String msg = isChecked ? "Recordatorios activados" : "Recordatorios desactivados";
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
     * Muestra un diálogo de seguridad para confirmar la eliminación de la cuenta.
     * Requiere que el usuario escriba una palabra clave de confirmación.
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
     * Ejecuta la petición de borrado de cuenta en el servidor.
     * Si el borrado es exitoso, cierra la sesión y redirige al Login.
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
     * Muestra un selector para cambiar el idioma de la aplicación.
     * Utiliza AppCompatDelegate para aplicar los cambios de localización de forma inmediata y global.
     */
    private void mostrarDialogoIdioma() {
        String[] idiomas = {"Español", "English", "Français"};
        String[] codigos = {"es", "en", "fr"};

        int seleccionado = 0;
        String idiomaActual = session.obtenerIdioma();
        for (int i = 0; i < codigos.length; i++) {
            if (codigos[i].equals(idiomaActual)) {
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
