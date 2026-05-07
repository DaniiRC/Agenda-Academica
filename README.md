<div align="center">
  <img src="https://img.icons8.com/color/144/000000/google-calendar--v2.png" alt="Agenda Académica Logo" width="120" height="120">
  <h1>📚 Agenda Académica</h1>
  <p><b>Gestor de tareas y productividad para estudiantes y profesores</b></p>

  <!-- Badges -->
  <a href="https://android.com"><img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot" alt="Spring Boot"></a>
  <a href="https://www.java.com/"><img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"></a>
  <a href="https://mysql.com/"><img src="https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"></a>
</div>

<br>

**Agenda Académica** es una solución de software completa (aplicación móvil nativa Android + API REST) diseñada como Proyecto Final para el Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM). 

Su objetivo es unificar la gestión del tiempo, fomentar la colaboración en aulas mediante un sistema de "Grupos/Clases", y mejorar la concentración del estudiante mediante herramientas integradas como el método Pomodoro.

---

## ✨ Características Principales

### 📱 Aplicación Android (Frontend)
- **Gestión de Tareas:** CRUD completo de eventos categorizables (Examen, Proyecto, Deberes, Estudio).
- **Grupos de Clase:** Sistema de invitación por código (Ej: `X7Y2Z`) para unirse a grupos de profesores y recibir eventos automáticamente en la agenda.
- **Temporizador Pomodoro:** Servicio en primer plano (*Foreground Service*) integrado en cada tarea para registrar el tiempo real invertido.
- **Modo Offline-First:** Base de datos local con `Room` para consultar la agenda sin conexión a internet.
- **Notificaciones Exactas:** Sistema de alarmas (`AlarmManager`) para recordar eventos en el momento preciso.
- **Widget de Escritorio:** Consulta tus tareas del día directamente desde la pantalla de inicio del teléfono.
- **Multi-idioma:** Soporte para Español e Inglés en tiempo de ejecución.

### ⚙️ Servidor API REST (Backend)
- **Seguridad Robusta:** Autenticación *stateless* basada en tokens **JWT**. Contraseñas cifradas con **BCrypt**.
- **Gestión de Archivos:** Endpoint multi-part para subida de fotos de perfil y portadas de clases.
- **Servicio de Emails:** Sistema de recuperación de contraseñas mediante códigos OTP enviados por SMTP (Gmail).
- **Sincronización:** Arquitectura diseñada para servir datos unificados (mezclando tareas personales y tareas de grupo).

---

## 🛠️ Stack Tecnológico

### Backend
* **Framework:** Spring Boot 4.0.1 (Java 21)
* **Persistencia:** Spring Data JPA / Hibernate
* **Base de Datos:** MySQL
* **Seguridad:** Spring Security 6 + JWT (`jjwt` v0.11.5)
* **Otros:** Jakarta Validation, Spring Boot Mail

### Android
* **Lenguaje:** Java (Min SDK 26, Target SDK 35)
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **Red:** Retrofit 2.9.0 + Gson
* **Caché Local:** Room Database 2.6.1
* **UI/UX:** Material Components, Lottie (Animaciones), Glide (Imágenes)
* **Servicios O.S:** AppWidgetProvider, Foreground Services, BroadcastReceivers

---

## 🚀 Arquitectura del Sistema

El proyecto sigue una arquitectura fuertemente desacoplada Cliente-Servidor:

```text
[ App Android (MVVM + Room) ]  <--- Retrofit (JSON + JWT) --->  [ API REST (Spring Boot) ]
        |                                                              |
  (Local Cache)                                                 (Persistencia)
  SQLite / Room                                                     MySQL
```

---

## 📖 Endpoints de la API (Resumen)

| Método | Ruta | Requiere JWT | Descripción |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/usuarios/login` | ❌ | Autenticación con email/contraseña. Devuelve JWT. |
| `POST` | `/api/usuarios/registro` | ❌ | Creación de cuenta nueva (contraseña hasheada). |
| `POST` | `/api/usuarios/google-login` | ❌ | Autenticación delegada vía Firebase. |
| `POST` | `/api/usuarios/enviar-codigo`| ❌ | Envío de OTP por email para recuperar contraseña. |
| `GET`  | `/api/eventos/usuario/{id}`  | ✅ | Obtiene tareas personales + tareas de grupos unidos. |
| `POST` | `/api/grupos/crear`          | ✅ | Crea una clase y devuelve un código de invitación. |
| `POST` | `/api/grupos/{codigo}/unirse`| ✅ | Une al usuario al grupo correspondiente al código. |
| `POST` | `/api/eventos/{id}/tiempo-invertido`| ✅ | Guarda el tiempo de estudio registrado por el Pomodoro. |

---

## ⚙️ Instalación y Despliegue

### 1. Despliegue del Backend
1. Clona el repositorio y abre la carpeta del backend en tu IDE (ej. IntelliJ IDEA).
2. Crea una base de datos en MySQL: `CREATE DATABASE agenda_pro;`
3. Configura las variables en `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/agenda_pro
   spring.datasource.username=root
   spring.datasource.password=TU_PASSWORD
   jwt.secret=CLAVE_SUPER_SECRETA_DE_AL_MENOS_256_BITS
   spring.mail.username=tu_email@gmail.com
   spring.mail.password=tu_password_de_aplicacion
   ```
4. Ejecuta el servidor: `mvn spring-boot:run`. El backend arrancará en `http://localhost:8080`.

### 2. Ejecución de la App Android
1. Abre la carpeta del proyecto Android en **Android Studio**.
2. Navega a la clase de configuración de red (ej. `RetrofitClient.java`).
3. Modifica la `BASE_URL` para que apunte a la IP de tu ordenador en la red local (fundamental para probar en dispositivos físicos):
   ```java
   public static final String BASE_URL = "http://192.168.1.X:8080";
   ```
4. Sincroniza Gradle y ejecuta la app en un emulador o dispositivo físico (Android 8.0+).

---

## 📸 Capturas de Pantalla

*(Nota: Añade tus capturas de pantalla en una carpeta `/docs/images/` y descomenta esta sección)*

<!--
<div align="center">
  <img src="docs/images/login.png" width="200" alt="Pantalla de Login"/>
  <img src="docs/images/agenda.png" width="200" alt="Agenda"/>
  <img src="docs/images/pomodoro.png" width="200" alt="Pomodoro"/>
  <img src="docs/images/grupo.png" width="200" alt="Perfil de Grupo"/>
</div>
-->

---

## 👨‍💻 Autor

**Daniel Ruiz Cocera**
* Proyecto Final - IES Las Fuentezuelas
* DAM 2º Curso (2025-2026)

---