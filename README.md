<div align="center">

<img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Retrofit-48B983?style=for-the-badge&logo=square&logoColor=white"/>
<img src="https://img.shields.io/badge/Room-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
<img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=material-design&logoColor=white"/>

<br/>
<br/>

# 📅 EduSync

**La aplicación de gestión de tareas y colaboración académica pensada para estudiantes y profesores.**  
Organiza tus exámenes, deberes y proyectos, colabora con tu clase, gestiona calificaciones individualizadas y mantén el foco con el temporizador Pomodoro integrado — todo funciona con sincronización remota y caché local sin conexión.

<br/>

[![Download APK](https://img.shields.io/badge/⬇_Descargar_APK-4F46E5?style=for-the-badge&logoColor=white)](https://landing-agenda-woad.vercel.app)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=for-the-badge)](https://android-arsenal.com/api?level=26)
[![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)](releases)

</div>

---

## ✨ Funcionalidades principales

| Función | Descripción |
|---|---|
| 📅 **Calendario + Tareas** | Crea y gestiona tareas clasificadas por tipo: *Examen, Deberes, Proyecto, Estudio*. Vista de calendario mensual y lista de tareas diarias. |
| 👥 **Grupos Académicos** | Los profesores pueden crear grupos y los alumnos unirse mediante un código único. Permite compartir tareas y entregas colaborativas. |
| ⏱ **Temporizador Pomodoro** | Sesiones de 25 minutos de foco inmersivo (*Focus Mode*). Implementado mediante un servicio en primer plano para funcionar en segundo plano. |
| 🔔 **Notificaciones Locales** | Alarmas y recordatorios exactos para cada una de tus tareas en la fecha y hora seleccionada. |
| 📴 **Modo Offline Integrado** | Persistencia local y caché mediante `Room Database`. La app funciona fluidamente sin conexión a internet y se sincroniza al recuperar la red. |
| 📊 **Notas y Estadísticas** | Registra calificaciones individualizadas de exámenes o tareas. La app calcula automáticamente la media ponderada del alumno y muestra métricas visuales. |

---

## 🏗 Arquitectura del Proyecto

El código fuente de la aplicación móvil Android se organiza bajo el namespace principal `com.example.edusync`:

```
com.example.edusync/
├── activities/                # Pantallas y flujos principales
│   ├── LoginActivity          # Pantalla de inicio de sesión JWT
│   ├── MainActivity           # Host del BottomNavigationView y vistas
│   ├── FormularioActivity     # Formulario de creación/edición de tareas
│   ├── DetalleEventoActivity  # Vista de detalles y checklist de tareas
│   ├── PerfilClaseActivity    # Panel de asignaturas asociadas
│   ├── ListaTareasActivity    # Historial de tareas pendientes y completadas
│   ├── EditarPerfilActivity   # Modificación de datos personales y foto
│   ├── AjustesActivity        # Gestión de idioma y preferencias
│   ├── CalificarAlumnosActivity # Calificación de alumnos individualizada por el profesor
│   └── ListaParticipantesActivity # Visualización y gestión de miembros del grupo
│
├── fragments/                 # Tabs principales de la interfaz
│   ├── AgendaFragment         # Vista calendario y tareas del día
│   ├── GrupoFragment          # Gestión y visualización de grupos
│   ├── MisClasesFragment      # Asignaturas del alumno/profesor
│   └── PerfilFragment         # Datos del usuario, foto y estadísticas de rendimiento
│
├── viewmodels/                # Capa de lógica y comunicación MVVM
│   ├── EventoViewModel
│   └── DataSyncViewModel
│
├── database/                  # Persistencia local (Room Database)
│   ├── AppDatabase
│   ├── EventoDao
│   ├── EventoEntity
│   └── EventoRepository       # Patrón repositorio con fallback local/remoto
│
├── network/                   # Cliente de red y servicios web
│   ├── RetrofitClient         # Configuración de URLs y serialización Gson
│   ├── ApiService             # Declaración de endpoints REST del backend
│   ├── AuthInterceptor        # Inyección automática del Token JWT en cabeceras
│   └── ErrorInterceptor       # Tratamiento centralizado de errores HTTP
│
├── services/                  # Servicios en segundo plano
│   ├── PomodoroService        # Foreground Service del temporizador de foco
│   └── NotificacionReceiver   # Receptor Broadcast para alarmas de tareas
│
└── utils/                     # Utilidades generales y helpers
    ├── SessionManager         # Persistencia de credenciales y roles con SharedPreferences
    ├── IdiomaUtils            # Internacionalización y cambio de idioma en runtime
    └── GlideUtils             # Carga optimizada de imágenes de perfil
```

La app implementa estrictamente el patrón de arquitectura **MVVM (Model-View-ViewModel)** de Google Jetpack para lograr un código desacoplado y mantenible:

```
View (Activity/Fragment)
    │  observa LiveData
    ▼
ViewModel
    │  solicita datos
    ▼
Repository ──── API REST (Retrofit) ──▶ Servidor Web (Render)
             └── Room DB (Caché local offline)
```

---

## 📋 Requisitos del Entorno

| Requisito | Versión Mínima / Recomendada |
|---|---|
| Sistema Operativo | Android 8.0 (API 26) o superior |
| Espacio en Disco | ~16 MB libres |
| Conectividad | Wi-Fi o Datos Móviles (Soporte offline incompleto) |

Para **compilar y depurar** el código fuente en tu entorno de desarrollo local:

| Herramienta | Versión Recomendada |
|---|---|
| Entorno de Desarrollo | Android Studio Hedgehog 2023.1 o superior |
| Java Development Kit | JDK 21 |
| Gradle Wrapper | Versión 8.x |
| Parámetros Gradle | `compileSdk: 36`, `minSdk: 26`, `targetSdk: 34` |

---

## 🚀 Instalación Rápida (APK)

1. Accede a la [Página Oficial de Descarga](https://landing-agenda-woad.vercel.app) del proyecto.
2. Escoge la variante deseada:
   * **APK en la Nube:** Conectada al servidor remoto (Render y Clever Cloud).
   * **APK en Local:** Conectada a tu instancia local de Spring Tools Suite en red Wi-Fi.
3. Activa en tu terminal Android: **Ajustes > Seguridad > Orígenes desconocidos** (o autoriza la instalación desde el navegador).
4. Abre el archivo descargado e instálalo en el dispositivo.

---

## 🔧 Guía de Configuración en Local

Si prefieres compilar la aplicación tú mismo desde el código fuente:

```bash
# 1. Clonar el repositorio del proyecto
git clone https://github.com/DaniiRC/Agenda-Academica.git
cd Agenda-Academica

# 2. Abrir en tu Android Studio
# File -> Open -> selecciona este directorio

# 3. Configurar la dirección del backend
# Edita el archivo: app/src/main/java/com/example/edusync/network/RetrofitClient.java
# Ajusta la variable BASE_URL con la IP de tu PC de desarrollo:
# public static final String BASE_URL = "http://192.168.1.XX:8080/";

# 4. Ejecutar el proyecto
# Conecta un emulador o un dispositivo físico y presiona Run (Shift + F10)
```

---

## 📦 Dependencias Tecnológicas Clave

```gradle
// Networking y REST API
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Base de Datos Local (Room)
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'

// Arquitectura MVVM y Ciclo de Vida
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'

// Procesamiento Gráfico de Imágenes
implementation 'com.github.bumptech.glide:glide:4.16.0'

// Animaciones Vectoriales Fluidas
implementation 'com.airbnb.android:lottie:6.1.0'

// Google Services y Firebase Auth (Opcional)
implementation 'com.google.firebase:firebase-auth:21.2.0'
```

---

## 🔐 Mecanismos de Autenticación y Seguridad

* **Acceso Tradicional:** Registro e Inicio de sesión protegido con credenciales de Email y Contraseña robustas (validadas bajo patrones de longitud, mayúsculas y números).
* **Filtros JWT:** Las credenciales validadas devuelven un token **JWT (JSON Web Token)** seguro generado por el servidor, que se almacena en el cliente de forma cifrada en `SharedPreferences` y es renovado de forma automática.
* **Control de Accesos (RBAC):** Restricción estricta de vistas y acciones según el rol del usuario (`ADMIN` para administración central, `PROFESOR` para gestionar grupos, calificar y editar, y `ALUMNO` para visualización y autogestión de su propia agenda).

---

<div align="center">
  Desarrollado por <strong>Daniel Ruiz Cocera</strong> · IES Las Fuentezuelas · 2ºDAM · 2026<br/>
  <a href="https://github.com/DaniiRC/Proyecto-Final-API">🔗 Repositorio del Backend REST (Spring Boot API)</a>
</div>
