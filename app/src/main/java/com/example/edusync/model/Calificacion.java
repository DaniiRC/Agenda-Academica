package com.example.edusync.model;

public class Calificacion {
    private Long id;
    private Long eventoId;
    private Long usuarioId;
    private String nombreUsuario;
    private Double nota;

    public Calificacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }
}
