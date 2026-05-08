<div align="center">

<img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Retrofit-48B983?style=for-the-badge&logo=square&logoColor=white"/>
<img src="https://img.shields.io/badge/Room-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
<img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=material-design&logoColor=white"/>

<br/>
<br/>

# 📅 Agenda Académica

**La app de gestión de tareas pensada para estudiantes.**  
Organiza tus exámenes, deberes y proyectos, colabora con tu clase y mantén el foco con el temporizador Pomodoro integrado — todo funciona aunque no tengas conexión.

<br/>

[![Download APK](https://img.shields.io/badge/⬇_Descargar_APK-4F46E5?style=for-the-badge&logoColor=white)](https://danirc.github.io/agenda-academica-web)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=for-the-badge)](https://android-arsenal.com/api?level=26)
[![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)](releases)

</div>

---

## ✨ Funcionalidades principales

| Función | Descripción |
|---|---|
| 📅 **Calendario + Tareas** | Crea y gestiona tareas por tipo: Examen, Deberes, Proyecto, Estudio. Vista de calendario mensual y lista diaria. |
| 👥 **Grupos Académicos** | Únete a la clase de tu profesor con un código único. Comparte tareas y entregas con todo el grupo. |
| ⏱ **Temporizador Pomodoro** | Sesiones de 25 minutos de foco puro. Servicio en primer plano, funciona con la app en segundo plano. |
| 🔔 **Notificaciones** | Alarmas exactas para recordarte cada tarea en el momento exacto que elijas. |
| 📴 **Modo Offline** | Room Database cachea todos tus datos localmente. La app funciona sin internet. |
| 🏠 **Widget** | Consulta las tareas del día directamente desde la pantalla de inicio sin abrir la app. |
| 📊 **Notas y estadísticas** | Registra tu nota obtenida y el peso de cada examen. Calcula tu media automáticamente. |
| 🔗 **Recursos URL** | Adjunta enlaces a materiales de estudio directamente en cada tarea. |

---

## 🏗 Arquitectura

```
com.example.agendaacademica/
├── activities/          # Pantallas principales
│   ├── LoginActivity
│   ├── MainActivity     # BottomNavigation host
│   ├── FormularioActivity
│   ├── DetalleEventoActivity
│   ├── PerfilClaseActivity
│   ├── ListaTareasActivity
│   ├── EditarPerfilActivity
│   └── AjustesActivity
│
├── fragments/           # Tabs del BottomNav
│   ├── AgendaFragment   # Calendario + lista diaria
│   ├── GrupoFragment    # Mis grupos
│   ├── MisClasesFragment# Asignaturas
│   └── PerfilFragment   # Cuenta del usuario
│
├── viewmodels/          # Capa MVVM
│   ├── EventoViewModel
│   └── DataSyncViewModel
│
├── database/            # Room (caché offline)
│   ├── AppDatabase
│   ├── EventoDao
│   ├── EventoEntity
│   └── EventoRepository
│
├── network/             # Retrofit + API
│   ├── RetrofitClient
│   ├── ApiService
│   ├── AuthInterceptor
│   └── ErrorInterceptor
│
├── services/            # Servicios Android
│   ├── PomodoroService  # Foreground service
│   └── NotificacionReceiver
│
├── widget/              # Widget pantalla inicio
│   ├── AgendaWidget
│   └── AgendaWidgetService
│
└── utils/
    ├── SessionManager   # JWT + SharedPreferences
    ├── IdiomaUtils      # i18n runtime
    └── GlideUtils       # Carga de imágenes
```

El proyecto sigue el patrón **MVVM** (Model–View–ViewModel):

```
View (Activity/Fragment)
    │  observa LiveData
    ▼
ViewModel
    │  solicita datos
    ▼
Repository ──── API REST (Retrofit) ──▶ Servidor
             └── Room DB (caché local)
```

---

## 📋 Requisitos previos

| Requisito | Versión mínima |
|---|---|
| Android | 8.0 (API 26) |
| Espacio en disco | ~30 MB |
| Conexión a internet | Opcional (posibilidad de ver tareas offline) |

Para **compilar el proyecto**:

| Herramienta | Versión |
|---|---|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 21 |
| Gradle | 8.x |
| compileSdk | 36 |

---

## 🚀 Instalación rápida (APK)

La forma más rápida de probar la app:

1. Ve a la [página de descarga](https://danirc.github.io/agenda-academica-web)
2. Pulsa **Descargar APK**
3. En tu Android: _Ajustes → Seguridad → Orígenes desconocidos_ → activar
4. Abre el APK descargado e instala
5. ¡Listo! Crea una cuenta y empieza a organizar tu semana

---

## 🔧 Compilar desde código fuente

```bash
# 1. Clonar el repositorio
git clone https://github.com/DaniiRC/Agenda-Academica.git
cd Agenda-Academica

# 2. Abrir en Android Studio
# File → Open → selecciona la carpeta

# 3. Configurar la URL del servidor
# Edita app/src/main/java/.../network/RetrofitClient.java
# BASE_URL = "https://tu-servidor.com/"   (o IP local para debug)

# 4. Compilar y ejecutar
# Run → Run 'app'  (o Shift+F10)
```

> **Nota:** La app apunta por defecto al servidor de producción en Clever Cloud. No necesitas levantar el backend para probarlo.

---

## 📦 Dependencias principales

```gradle
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Base de datos local
implementation 'androidx.room:room-runtime:2.6.1'

// Arquitectura MVVM
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'

// Imágenes
implementation 'com.github.bumptech.glide:glide:4.16.0'

// Animaciones
implementation 'com.airbnb.android:lottie:6.1.0'

// Google Sign-In
implementation 'com.google.firebase:firebase-auth:21.2.0'
```

---

## 🔐 Autenticación

La app soporta tres métodos de acceso:

- **Email + contraseña** — con recuperación por código de 6 dígitos
- **JWT** — token almacenado en `SessionManager` (SharedPreferences), válido 24 horas

---

<div align="center">
  Desarrollado por <strong>Daniel Ruiz Cocera</strong> · IES Las Fuentezuelas · 2ºDAM · 2026<br/>
  <a href="https://github.com/DaniiRC/Proyecto-Final-API">🔗 Repositorio del Backend (API REST)</a>
</div>
