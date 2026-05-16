package com.example.edusync.model;

import java.util.List;

public class Evento {
    private Long id;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String hora;
    private String tipo;
    private Double pesoNota;
    private Integer dificultad;
    private Integer horasEstimadas;
    private List<Subtarea> subtareas;
    private Asignatura asignatura;
    private Usuario creador;
    private boolean completado;
    @com.google.gson.annotations.SerializedName(value = "focusMode", alternate = {"focus_mode", "focus", "isFocusMode", "focus_mode_enabled", "pomodoro"})
    private boolean focusMode;
    @com.google.gson.annotations.SerializedName(value = "dndEnabled", alternate = {"dnd_enabled", "no_molestar"})
    private boolean dndEnabled;
    private Long tiempoRestanteFocus; // Lo que queda por estudiar
    private Long tiempoInvertidoFocus; // Lo que YA se ha estudiado (real)
    private List<String> recursosUrls;
    private Grupo grupo;
    private Double notaObtenida;

    public Evento() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getPesoNota() { return pesoNota; }
    public void setPesoNota(Double pesoNota) { this.pesoNota = pesoNota; }

    public Integer getDificultad() { return dificultad; }
    public void setDificultad(Integer dificultad) { this.dificultad = dificultad; }

    public Integer getHorasEstimadas() { return horasEstimadas; }
    public void setHorasEstimadas(Integer horasEstimadas) { this.horasEstimadas = horasEstimadas; }

    // Alias profesional para el frontend
    public Integer getTiempoEstudio() { return horasEstimadas; }
    public void setTiempoEstudio(Integer tiempoEstudio) { this.horasEstimadas = tiempoEstudio; }

    public Asignatura getAsignatura() { return asignatura; }
    public void setAsignatura(Asignatura asignatura) { this.asignatura = asignatura; }

    public Usuario getCreador() { return creador; }
    public void setCreador(Usuario creador) { this.creador = creador; }

    public List<Subtarea> getSubtareas() { return subtareas; }
    public void setSubtareas(List<Subtarea> subtareas) { this.subtareas = subtareas; }

    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }

    public boolean isFocusMode() { return focusMode; }
    public void setFocusMode(boolean focusMode) { this.focusMode = focusMode; }

    public boolean isDndEnabled() { return dndEnabled; }
    public void setDndEnabled(boolean dndEnabled) { this.dndEnabled = dndEnabled; }

    public Long getTiempoRestanteFocus() { return tiempoRestanteFocus; }
    public void setTiempoRestanteFocus(Long tiempoRestanteFocus) { this.tiempoRestanteFocus = tiempoRestanteFocus; }

    public Long getTiempoInvertidoFocus() { return tiempoInvertidoFocus; }
    public void setTiempoInvertidoFocus(Long tiempoInvertidoFocus) { this.tiempoInvertidoFocus = tiempoInvertidoFocus; }

    public List<String> getRecursosUrls() { return recursosUrls; }
    public void setRecursosUrls(List<String> recursosUrls) { this.recursosUrls = recursosUrls; }

    public Double getNotaObtenida() { return notaObtenida; }
    public void setNotaObtenida(Double notaObtenida) { this.notaObtenida = notaObtenida; }
}
