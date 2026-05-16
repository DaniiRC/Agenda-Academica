package com.example.edusync.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.edusync.fragments.EduSyncFragment;
import com.example.edusync.fragments.GrupoFragment;
import com.example.edusync.fragments.MisClasesFragment;
import com.example.edusync.fragments.PerfilFragment;
import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.lifecycle.ViewModelProvider;
import com.example.edusync.viewmodel.DataSyncViewModel;

/**
 * Actividad principal de la aplicación que actúa como contenedor para los fragmentos de navegación.
 * Gestiona la navegación inferior, la sincronización inicial de datos y la verificación de permisos
 * críticos del sistema.
 */
public class MainActivity extends BaseActivity {

    private FragmentManager fm;
    private SessionManager session;
    private DataSyncViewModel dataSyncViewModel;
    private static final int REQUEST_CODE_FORMULARIO = 1001;

    /**
     * Inicializa la actividad, verifica la sesión del usuario y configura el sistema de navegación.
     * También solicita permisos de notificaciones y alarmas exactas según la versión de Android.
     * 
     * @param savedInstanceState Estado previo de la actividad para restaurar datos si es necesario.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.estaLogueado()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        aplicarPreferenciaTema();
        setContentView(R.layout.activity_main);

        dataSyncViewModel = new ViewModelProvider(this).get(DataSyncViewModel.class);
        fm = getSupportFragmentManager();
        
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            setupFragments();
        }

        configurarNavegacion(bottomNav);
        configurarGestorAtras(bottomNav);
        verificarPermisosCriticos();
    }

    /**
     * Aplica el modo de noche según la preferencia guardada en la sesión del usuario.
     */
    private void aplicarPreferenciaTema() {
        if (session.esModoClaro()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /**
     * Configura el listener para la barra de navegación inferior.
     * 
     * @param bottomNav Instancia del BottomNavigationView.
     */
    private void configurarNavegacion(BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target = fm.findFragmentByTag(String.valueOf(id));
            if (target != null) {
                showFragment(target);
                return true;
            }
            return false;
        });
    }

    /**
     * Configura el comportamiento del botón atrás para retornar a la agenda antes de salir.
     * 
     * @param bottomNav Instancia del BottomNavigationView para controlar el estado seleccionado.
     */
    private void configurarGestorAtras(BottomNavigationView bottomNav) {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNav.getSelectedItemId() != R.id.nav_agenda) {
                    bottomNav.setSelectedItemId(R.id.nav_agenda);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * Verifica y solicita permisos de notificaciones (Android 13+) y alarmas exactas (Android 12+).
     */
    private void verificarPermisosCriticos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            outState.putInt("SELECTED_TAB", bottomNav.getSelectedItemId());
        }
    }

    /**
     * Inicializa los fragmentos principales y los añade al contenedor, ocultando los secundarios.
     */
    private void setupFragments() {
        Fragment agenda = new EduSyncFragment();
        Fragment grupo = new GrupoFragment();
        Fragment clases = new MisClasesFragment();
        Fragment perfil = new PerfilFragment();

        fm.beginTransaction().add(R.id.fragment_container, perfil, String.valueOf(R.id.nav_perfil)).hide(perfil).commit();
        fm.beginTransaction().add(R.id.fragment_container, clases, String.valueOf(R.id.nav_mis_clases)).hide(clases).commit();
        fm.beginTransaction().add(R.id.fragment_container, grupo, String.valueOf(R.id.nav_grupo)).hide(grupo).commit();
        fm.beginTransaction().add(R.id.fragment_container, agenda, String.valueOf(R.id.nav_agenda)).commit();
    }

    /**
     * Cambia la visibilidad entre fragmentos para optimizar la carga y mantener el estado.
     * 
     * @param fragment El fragmento que se desea mostrar.
     */
    private void showFragment(Fragment fragment) {
        if (fragment == null) return;
        
        var transaction = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isVisible()) {
                transaction.hide(f);
            }
        }
        transaction.show(fragment).commit();
    }
}
