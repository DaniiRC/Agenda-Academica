package com.example.edusync.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.edusync.R;
import com.example.edusync.SessionManager;
import com.example.edusync.activities.AjustesActivity;
import com.example.edusync.adapter.GrupoAdapter;
import com.example.edusync.model.Grupo;
import com.example.edusync.network.ApiService;
import com.example.edusync.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento que muestra la lista de clases académicas a las que pertenece el usuario.
 * Incluye búsqueda en tiempo real por nombre y un contador de clases activas.
 */
public class MisClasesFragment extends Fragment {

    private GrupoAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvContador;
    private View layoutSinClases;
    private List<Grupo> listaClases = new ArrayList<>();

    private ApiService apiService;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_misclases, container, false);

        apiService = RetrofitClient.getApiService();
        session = new SessionManager(requireContext());

        initViews(view);
        return view;
    }

    private void initViews(View v) {
        RecyclerView rvClases = v.findViewById(R.id.recyclerAsignaturas);
        EditText etBuscador = v.findViewById(R.id.etBarraBusqueda);
        swipeRefresh = v.findViewById(R.id.swipeRefreshClases);
        tvContador = v.findViewById(R.id.tvContadorClases);
        layoutSinClases = v.findViewById(R.id.tvSinClases);
        ImageButton btnSettings = v.findViewById(R.id.btn_settings);

        rvClases.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GrupoAdapter(new ArrayList<>());
        rvClases.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::cargarClasesDelUsuario);

        btnSettings.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), AjustesActivity.class);
            startActivity(intent);
        });

        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarClasesDelUsuario();
    }

    private void cargarClasesDelUsuario() {
        long usuarioId = session.getUserId();
        if (usuarioId == -1L) {
            detenerRefresco();
            return;
        }

        if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);

        apiService.obtenerGruposDeUsuario(usuarioId).enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Grupo>> call, @NonNull Response<List<Grupo>> response) {
                detenerRefresco();
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    listaClases = response.body();
                    adapter.updateList(listaClases);
                    actualizarUI();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Grupo>> call, @NonNull Throwable t) {
                detenerRefresco();
            }
        });
    }

    private void actualizarUI() {
        if (tvContador != null) {
            tvContador.setText(getString(R.string.clases_formato, listaClases.size()));
        }
        if (layoutSinClases != null) {
            layoutSinClases.setVisibility(listaClases.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void detenerRefresco() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }
}
