package com.example.agendaacademica.model;

/**
 * DTO que representa la respuesta del backend al hacer login o registro.
 * Contiene el JWT y los datos básicos del usuario.
 */
public class LoginResponse {

    private String token;
    private Long id;
    private String nombre;
    private String email;
    private String fotoUrl;
    private String rol;

    public LoginResponse() {}

    // Getters
    public String getToken()   { return token;   }
    public Long   getId()      { return id;      }
    public String getNombre()  { return nombre;  }
    public String getEmail()   { return email;   }
    public String getFotoUrl() { return fotoUrl; }
    public String getRol()     { return rol;     }

    // Setters
    public void setToken(String token)     { this.token   = token;   }
    public void setId(Long id)             { this.id      = id;      }
    public void setNombre(String nombre)   { this.nombre  = nombre;  }
    public void setEmail(String email)     { this.email   = email;   }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setRol(String rol)         { this.rol     = rol;     }
}
