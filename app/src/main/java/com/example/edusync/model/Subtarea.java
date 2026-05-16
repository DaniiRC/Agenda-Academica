package com.example.edusync.model;

/**
 * Modelo de Subtarea sincronizado con el Backend (Spring Boot)
 */
public class Subtarea {
    private Long id;
    private String titulo;
    private boolean completada;

    public Subtarea() {}

    public Subtarea(String titulo) {
        this.titulo = titulo;
        this.completada = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }

    // Métodos de compatibilidad para evitar errores en otras partes del código
    public String getDescripcion() { return titulo; }
    public void setDescripcion(String descripcion) { this.titulo = descripcion; }
}
