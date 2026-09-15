# CONTEXTO DE TRASPASO — ONE Cognitive Companion

> Documento preparado para continuar el desarrollo de ONE desde otro PC.
> Leerlo completo antes de modificar código. Este archivo es un contexto de
> trabajo, no sustituye a los README ni al contrato OpenAPI versionado.

Fecha de actualización: 2026-09-15
Proyecto local de Codex: Hackathon
Carpeta contenedora actual en este PC: C:\Users\Mauro\Desktop\develop\ONE
Producto: ONE Cognitive Companion
Estado general: MVP multi-cliente local-first, con backend FastAPI, cliente
iOS nativo, cliente Android nativo, dashboard web y documentación Vocs.
Estado de esta sesión: se ha completado una primera equiparación de la interfaz
Android con iOS; los cambios de código Android y esta actualización de contexto
deben revisarse y publicarse antes de continuar en otra máquina.

---

## 1. Instrucciones prioritarias para el siguiente agente

Estas reglas son obligatorias salvo que el usuario las cambie expresamente:

1. NO modificar el código de las APIs del repositorio backend. En este
   checkout el repositorio está en la carpeta `one` (en conversaciones antiguas
   puede aparecer como `one-backend`). Ese repositorio lo mantiene otro
   compañero. Puede inspeccionarse, ejecutarse y llamarse desde los clientes,
   pero no se deben editar sus archivos desde este flujo de trabajo salvo
   autorización expresa y coordinación.
2. one-android/app/src/main/java/com/one/cognitivecompanion/OneApi.kt es el
   adaptador Android del contrato. Se puede modificar si hace falta para
   adaptar Android, corregir el mapeo cliente o añadir soporte a endpoints ya
   existentes, pero no se deben cambiar los endpoints ni sus contratos sin
   coordinarlo con la persona responsable del backend.
3. No inventar endpoints nuevos ni cambiar nombres, cuerpos o semántica de
   endpoints desde Android. Si el contrato no permite una funcionalidad, se
   debe explicar y pedir autorización antes de ampliar el backend.
4. No mostrar nunca tokens, contraseñas, claves API, códigos de pairing,
   certificados ni valores reales de .env en respuestas, commits o archivos
   de contexto.
5. Mantener la experiencia en inglés salvo que el usuario pida localización.
   Los textos deben ser naturales, breves y coherentes con ONE. No presentar
   observaciones como diagnósticos ni medicación como consejo médico.
6. Antes de una modificación importante, comprobar el estado del repositorio
   concreto. Después, ejecutar pruebas proporcionales y dejar claro qué se
   verificó.
7. No hacer git reset --hard, git checkout -- ni borrar archivos para
   resolver conflictos sin autorización explícita.

La prioridad actual es continuar principalmente en one-android. El backend es
la fuente de verdad del contrato; iOS y frontend sirven como referencia
funcional y visual.

---

## 2. Estructura real del proyecto y Git

La carpeta ONE es una carpeta contenedora y no es un repositorio Git. No se debe
ejecutar git pull en ONE. Cada subcarpeta principal es un repo independiente.
En este checkout, el repositorio backend se llama `one` (en otros equipos puede
haberse elegido otro nombre local):

- one-android: app Android Kotlin/Compose
  Remoto: https://github.com/maualc/one-android
- one: API FastAPI y servicios locales
  Remoto: https://github.com/0xbiel/one.git
- one-frontend: dashboard/publisher React + Vite
  Remoto: https://github.com/0xbiel/one-frontend.git
- one-ios: app iOS SwiftUI/RoomPlan
  Remoto: https://github.com/0xbiel/one-ios.git
- one-docs: documentación Vocs
  Remoto: https://github.com/0xbiel/one-docs.git

También existe tmp, con PDFs, vídeos, imágenes y textos descargados para el
análisis del hackathon. tmp no es necesario para compilar la aplicación y
contiene archivos grandes; no incluirlo en un repo ni subirlo salvo que una
tarea concreta lo necesite.

### Estado observado en este PC

Todos los repositorios están en `main` y alineados con `origin/main`. El único
repositorio con cambios locales es `one-android`, donde la equiparación de UI
descrita más abajo todavía no tiene commit ni push.

Commits relevantes observados:

- one-android — `2cf4629 feat(android): add 2D room mapping from camera walkthroughs`
  (HEAD y origin/main antes de los cambios de UI).
- one — `9cac758 Add guided person camera calibration` (backend, limpio).
- one-frontend — `9118b9b Add guided standing-point calibration` (limpio).
- one-ios — `2c67c37 Fix care-recipient medication UUID encoding` (limpio).
- one-docs — `61f5d0d Document care-recipient medication flow` (limpio).

Cambios locales pendientes en `one-android`:

- `app/src/main/java/com/one/cognitivecompanion/OneApp.kt`: equiparación de
  navegación, Home, Family, Assistant, Map y Account con la disposición de iOS.
- `CONTEXTO_ONE_PORTATIL.md`: este documento de traspaso.

Este archivo está dentro de `one-android` y está versionado por ese repositorio;
después de hacer commit y push se podrá recuperar con el pull de Android. Si se
trabaja antes de publicar, copiarlo manualmente al otro PC. La carpeta ONE
contenedora sigue sin ser un repositorio y nunca se debe hacer `git add .` desde
ella.

---

## 3. Cómo preparar el portátil

### Opción A: los repos ya están clonados

Abrir PowerShell en la carpeta contenedora del portátil y actualizar cada repo
por separado. Usar --ff-only para no crear merges automáticos inesperados:

    cd C:\ruta\al\ONE
    git -C one-android pull --ff-only origin main
    git -C one pull --ff-only origin main
    git -C one-frontend pull --ff-only origin main
    git -C one-ios pull --ff-only origin main
    git -C one-docs pull --ff-only origin main

Si alguno tiene cambios locales, detenerse y revisar primero:

    git -C one-android status --short --branch

No sobrescribir cambios locales sin confirmar con el usuario.

### Opción B: clonar desde cero

    mkdir C:\ruta\al\ONE
    cd C:\ruta\al\ONE
    git clone https://github.com/maualc/one-android.git one-android
    git clone https://github.com/0xbiel/one.git one
    git clone https://github.com/0xbiel/one-frontend.git one-frontend
    git clone https://github.com/0xbiel/one-ios.git one-ios
    git clone https://github.com/0xbiel/one-docs.git one-docs

Copiar después este archivo a la raíz del nuevo ONE si no se ha publicado aún.
También se puede abrir la carpeta raíz en Codex para que el agente vea los cinco
repos y este contexto.
En Android Studio se debe abrir específicamente one-android, no la carpeta
contenedora.

### Requisitos aproximados

- Android Studio compatible con AGP 9.4.0, Gradle JDK 17 o superior (se
  recomienda el JBR incluido en Android Studio) y Android SDK compile 37.
  El código mantiene Java source/target 11; eso no significa que Gradle pueda
  ejecutarse con un JDK 11 antiguo.
- Un emulador Android API 36 o un dispositivo físico para pruebas reales.
- Python 3.12 o superior para backend.
- Node.js moderno; one-docs declara Node >=22.15.
- Docker Desktop si se quiere usar Compose con PostgreSQL, Redis, MinIO,
  LiveKit y Caddy.
- Xcode 26.2 o posterior únicamente para compilar one-ios en macOS.

---

## 4. Qué es ONE

ONE es un acompañante cognitivo basado en IA personalizada para personas con
MCI, Alzheimer, demencia y necesidades relacionadas, con apoyo a cuidadores y
familiares. El enfoque del MVP es local-first, con consentimiento explícito,
incertidumbre visible y separación de roles.

La aplicación no debe sustituir a la persona, al cuidador, al médico ni al
equipo de cuidados. Las observaciones de visión son señales aproximadas; no
son diagnósticos. La medicación se organiza administrativamente: nombre, dosis
introducida por el usuario, horarios y estados de check-in. No se debe generar
consejo clínico ni decidir automáticamente una dosis.

### Principios funcionales

- El usuario controla qué audio, vídeo, familia y medicación autoriza.
- El backend aplica autenticación, consentimiento, pertenencia al hogar y
  permisos por rol.
- Los frames de cámara se procesan de forma acotada y en memoria; no se debe
  presentar una retención de vídeo que no exista.
- Los mapas tienen procedencia explícita: zonas manuales, geometría relativa,
  ARKit/RoomPlan y mapas RoomPlan/LiDAR métricos no son equivalentes.
- La vista del cuidador puede revisar hogar, cámaras, mapa, eventos, clips,
  familiares, planes y privacidad según sus permisos.
- La vista del residente debe ser simple y calmada: Today muestra las próximas
  rutinas/medicaciones y Assistant se centra en check-in y conversación.
- La vista Publisher se limita a publicar la cámara/micrófono consentidos y no
  debe obtener controles de cuidador, familia o medicación.

---

## 5. Estado actual de Android (one-android)

### Stack

- Kotlin 2.2.10.
- Jetpack Compose y Material 3.
- Android Gradle Plugin 9.4.0.
- Gradle wrapper incluido.
- compileSdk 37, targetSdk 37, minSdk 26.
- Java source/target 11.
- CameraX 1.4.2 para capturas acotadas.
- LiveKit Android 2.28.2 y componentes Compose 2.4.2.
- Media3 1.11.0 para reproducción de clips.
- applicationId: com.one.cognitivecompanion.
- namespace: com.one.cognitivecompanion.

### Archivos principales

- app/src/main/java/com/one/cognitivecompanion/MainActivity.kt: entrada de la
  actividad.
- OneApp.kt: shell Compose, autenticación, navegación, pantallas, diálogos,
  textos y componentes visuales. Es grande; localizar símbolos antes de editar.
- OneAppState.kt: estado y coordinación de sesión, hogar, familia,
  medicación, mapas, eventos, notificaciones, logout y caché.
- OneApi.kt: modelos, sesiones y cliente HTTP del contrato. Puede editarse
  cuando la tarea sea específica del cliente Android; preservar siempre la
  compatibilidad con el backend.
- OneModels.kt: modelos auxiliares de la app.
- OneHomeRepository.kt: carga de hogar y datos relacionados.
- OneFamilyRepository.kt: flujos de familia/care circle.
- OneMedicationRepository.kt: acceso local/organización de medicación.
- OneMedicationNotifications.kt: canales, alarmas y notificaciones de dosis.
- OneOfflineCache.kt: datos acotados en caché local.
- OneSecureStore.kt: almacenamiento seguro de sesión.
- OneCameraRepository.kt: cámara y capturas.
- OneCaptureService.kt: servicio de captura/publicación.
- OneLiveKitPublisher.kt: conexión LiveKit del publisher.
- OneEventStream.kt: consumo de eventos.
- OneMapImport.kt: importación de JSON de mapas/zones.
- OneSpeechRecognizer.kt: reconocimiento de voz.
- OneFormatting.kt: formato de fechas, etiquetas y valores.
- ui/theme/Color.kt, Theme.kt, Type.kt: sistema visual.

### Roles y navegación

El cliente distingue, como mínimo, los roles Caregiver, Resident y Publisher.
El rol de la cuenta y una acción para cambiar la vista no deben confundirse:

- En una fila de miembro, la cuenta actual debe mostrar YOU junto al nombre
  grande y la etiqueta del rol real, por ejemplo Caregiver.
- No mostrar el email en las filas de acceso de familia si el diseño actual no
  lo pide.
- Resident puede ser una vista/acción para otro miembro; no implica que la
  cuenta actual tenga ese rol.
- El publisher no debe ganar por accidente la navegación del cuidador ni el
  acceso a datos familiares.

La navegación actual del cuidador está alineada con el shell de iOS y contiene
cinco áreas en la barra inferior: Home, Map, Family, Assistant y Account.
Events ya no ocupa una pestaña propia: se abre desde “See all” o desde los
eventos recientes de Home como una pantalla secundaria, y desde allí se puede
abrir el detalle de un evento. Las pantallas de cámara, evento y Events ocultan
la barra inferior; el botón/gesto atrás cierra primero el detalle y después la
pantalla secundaria.

Todas las pestañas usan una transición Compose direccional con fade y
desplazamiento horizontal. La transición se calcula con el orden visible de
cada rol, por lo que también funciona para Resident y Publisher. El cambio de
rol debe seguir siendo una acción de demo/vista y no alterar accidentalmente
los permisos reales de la cuenta.

La Home activa tiene acciones reales: el contexto de care space abre el
selector, el plan de hoy abre Family, Map abre el mapa, Cameras abre la gestión
de cámaras, “See all” abre Events y cada evento abre su detalle. No dejar una
flecha visual en una fila que no tenga `onClick` o una acción accesible
equivalente.

La pantalla de residente tiene Today, Assistant y Account. Today muestra el
resumen del día y los recordatorios de medicación con hora, instrucciones y
estado cuando existan. Assistant permanece centrado en conversación/check-in y
remite a Today para las pastillas.

### Diseño visual ya aplicado

- Barra inferior blanca, con la pestaña seleccionada en azul ONE y contenido
  seleccionado blanco.
- Paneles popup/AlertDialog blancos, con esquinas amplias, campos redondeados,
  espaciado consistente y botones azules ONE.
- El panel grande de notificaciones refleja el permiso real de Android y el
  estado concedido; no añadir un mensaje inferior redundante.
- Splash/icono de Android usa el logo de ONE, no el robot verde de plantilla.
- El nombre actual y YOU deben permanecer alineados en las filas de familia.
- Mantener una interfaz calmada, blanca, con tipografía amplia, contornos
  discretos y azul/cian ONE. Evitar introducir morados o colores de plantilla.

### Equiparación Android con la interfaz de iOS (15-09-2026)

La implementación actual está concentrada en `OneApp.kt` y conserva los
repositorios, modelos, consentimientos y endpoints existentes. Se ha replicado
la jerarquía funcional de `one-ios` sin intentar copiar literalmente los
patrones de SwiftUI:

- Home: saludo personalizado, persona atendida, logo, contexto del care space,
  estado offline/demo, tarjeta de Today, accesos compactos a Map y Cameras,
  eventos recientes y pairing del publisher. El selector de care space y la
  gestión completa de cámaras se muestran en `ModalBottomSheet` para no hacer
  crecer la Home.
- Family: selector de persona y avisos de consentimiento arriba; después
  destinatarios de cuidado, plan/recordatorios de medicación, historial y,
  finalmente, acceso de miembros e invitaciones. El asistente familiar ya no se
  mezcla dentro de esta pantalla.
- Assistant: nueva pantalla propia para el cuidador, con selector de persona,
  historial local de preguntas, resultado acotado a planes/check-ins,
  limitaciones visibles y compositor inferior que respeta el teclado. El envío
  solo se habilita con sesión backend, sujeto seleccionado y consentimiento
  `family_assistant` activo.
- Map: el canvas 2D y la calidad del mapa aparecen antes que las herramientas.
  Room scan, ARCore, configuración, calibración, evidencias, importación,
  mapas manuales, objetos y observaciones quedan agrupados en “Map tools and
  evidence” y se despliegan bajo demanda.
- Account: contexto de care space al principio con selector en hoja inferior;
  después privacidad/consentimientos y notificaciones, estado secundario del
  backend, exportación/borrado, sesión y la vista demo de otro rol.
- Events: se mantiene el listado y el detalle ya existentes, pero el listado es
  un destino secundario desde Home, no una pestaña inferior independiente.

Se conservaron los estados de carga, error, stale/offline y demo, además de las
acciones existentes de cámaras, pairing, clips, medicación, familia y mapas.
No se modificó `OneApi.kt` ni el backend para esta equiparación.

### Correcciones ya realizadas en Android

Los siguientes commits forman parte de origin/main si el portátil actualiza
one-android:

- 3dce0d1: barra inferior blanca y selección azul ONE.
- a0fd43d: estado correcto del panel grande de notificaciones y eliminación
  del texto inferior redundante.
- a17f6ce: separación entre Today del residente y Assistant; Today muestra
  próxima medicación, lista de recordatorios, estados de carga/error y ausencia
  de recordatorios.
- 24dc67d: acciones de Household status y This week's plan, con accesibilidad
  y feedback táctil.
- 416bacc: estilo ONE del diálogo de creación/edición del plan de medicación.
- 17e3430: estilo unificado para todos los popup/dialogs.
- 7cefee4: aclaración del rol de la cuenta actual en familia.
- 6be3791: alineación de identidad: YOU al lado del nombre grande y email
  oculto.
- 50ed0da: revisión general de UI y fiabilidad Android.
- 2cf4629: mapeado 2D de habitaciones a partir de recorridos de cámara,
  incluyendo generación, calibración, evidencias, importación y mapas manuales.

### Cambios de UI pendientes de commit

Después de `2cf4629`, se modificó `app/src/main/java/com/one/cognitivecompanion/OneApp.kt`
para completar la equiparación Android/iOS descrita arriba. La modificación aún
está en el working tree de `main`; no hacer `pull` en otro equipo hasta haber
hecho commit y push, o copiar explícitamente el archivo si se necesita seguir
sin publicar.

La revisión final comunicada para 50ed0da indicó:

- testDebugUnitTest, assembleDebug, lintDebug y assembleRelease OK.
- Tests instrumentados en Pixel 9 API 36 OK.
- Lint sin errores; quedaban únicamente avisos de versiones/dependencias.
- Backend sin cambios.
- Se añadieron/ajustaron vuelta atrás, logout de publisher, reconciliación de
  alarmas, protección de caché/backup, splash, tarjetas, pestañas, demo y
  textos.
- OneApi.kt quedó intacto en esa revisión, pero no es una prohibición
  permanente: puede adaptarse en tareas Android sin modificar el backend.

No asumir que build correcta significa que todos los flujos físicos estén
verificados: permisos, cámara/WebRTC, LiveKit, notificaciones y dispositivos
reales siguen dependiendo del entorno.

---

## 6. Android: configuración y comandos

### Endpoint de API

El valor por defecto de debug está pensado para el emulador:

    http://10.0.2.2:8000/api/v1

Para otro endpoint, no escribirlo permanentemente en código ni incluir
secretos. Pasarlo como propiedad de Gradle:

    cd C:\ruta\al\ONE\one-android
    .\gradlew.bat assembleDebug -PoneApiBaseUrl=https://dev.example/api/v1

Release usa un placeholder inválido si no se proporciona un endpoint. Antes
de empaquetar una release real hay que suministrar una URL HTTPS válida:

    .\gradlew.bat bundleRelease -PoneApiBaseUrl=https://one.example/api/v1

### Verificación recomendada

Desde one-android:

    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat lintDebug
    .\gradlew.bat assembleDebug
    .\gradlew.bat assembleRelease -PoneApiBaseUrl=https://configure-me.invalid/api/v1

Última verificación realizada en este PC (15-09-2026):

    $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio1\jbr'
    .\gradlew.bat :app:compileDebugKotlin
    .\gradlew.bat :app:testDebugUnitTest
    .\gradlew.bat :app:assembleDebug

Las tres tareas terminaron con `BUILD SUCCESSFUL`. El APK se generó en
`app/build/outputs/apk/debug/app-debug.apk`. Si Android Studio muestra “Project
JDK is not defined”, abrir `Setup SDK` y elegir el `jbr-25 JetBrains Runtime`
incluido, o usar `Add JDK from disk` apuntando a la carpeta `jbr` de la
instalación de Android Studio. La ruta exacta cambia según el PC.

El test instrumentado requiere un emulador/dispositivo conectado:

    .\gradlew.bat connectedDebugAndroidTest

No editar local.properties; es específico de cada PC y normalmente contiene la
ruta local del SDK. No subir keystores, firmas ni credenciales.

### Notificaciones

Las reglas de horarios del backend pueden ser diarias, semanales o fechadas.
Ejemplos documentados:

- 08:00,20:00
- Mon,Wed,Fri @ 08:00
- weekdays 08:00
- weekends 20:00
- 2026-09-12 @ 08:00

La app debe respetar el día indicado y no programar una dosis ya marcada como
taken, skipped o missed. Al editar/desactivar un plan, reconciliar las alarmas
antiguas. Al cerrar sesión, cancelar las alarmas de la sesión anterior cuando
corresponda. Esto es lógica de cliente; no cambiar el parser o el contrato del
backend sin autorización.

### Privacidad Android

El caché puede contener eventos, cámaras, mapas y recordatorios. Mantenerlo
acotado, limpiar al cerrar sesión y revisar las reglas de backup. No activar
copias de seguridad de datos sensibles por defecto. El vídeo debe permanecer
transitorio según el diseño y los avisos de la UI deben reflejar si la cámara
es local o una vista LiveKit remota.

---

## 7. Backend (carpeta local `one`): referencia de solo lectura

El backend es un monolito modular FastAPI para el MVP local-first. Usa
PostgreSQL como base autoritativa en Compose y SQLite como fallback de cero
configuración para desarrollo/tests.

### Componentes

- app/main.py: aplicación FastAPI, validación, autenticación y rutas.
- app/config.py: configuración por variables de entorno.
- app/db.py: acceso a base de datos.
- app/security.py: sesiones, hashes y controles de seguridad.
- app/events.py: bus/eventos.
- app/media.py: buffer acotado y clips locales cifrados.
- app/vision.py: estabilización temporal y proyección de detecciones.
- app/roomplan.py: modelos RoomPlan/AR video.
- app/geometry.py: cliente del servicio de geometría.
- app/integrations.py: adapter de LM Studio/OpenAI-compatible.
- geometry_service/: inferencia de geometría/localización.
- migrations/: migraciones numeradas de PostgreSQL.
- contracts/openapi.json: contrato cliente versionado.
- tests/: pruebas del backend.

### Familias de rutas existentes

El contrato versionado vive bajo /api/v1. Entre otras, cubre:

- health, sesión actual, email auth y pairing;
- care spaces/homes y destinatarios de cuidado;
- consentimientos, pausa, exportación y borrado de privacidad;
- cámaras, pairing de dispositivos y reconexión;
- habitaciones, mapas manuales, RoomPlan y ARKit video;
- visual landmarks, localización de cámara y calibración;
- objetos, detecciones/vision frames, observaciones y eventos;
- clips y contenido de clips;
- miembros de familia, invitaciones y roles;
- planes de medicación, recordatorios y check-ins;
- family assistant acotado a planes/check-ins;
- stream SSE de eventos;
- token LiveKit y webhook.

Rutas especialmente relevantes para Android:

    GET    /api/v1/health
    GET    /api/v1/me
    POST   /api/v1/auth/email/request
    POST   /api/v1/auth/email/verify
    POST   /api/v1/pairing/start
    POST   /api/v1/pairing/complete
    DELETE /api/v1/sessions/current
    GET    /api/v1/homes/{home_id}/family/members
    GET    /api/v1/homes/{home_id}/medication-plans
    GET    /api/v1/homes/{home_id}/medication-reminders
    POST   /api/v1/homes/{home_id}/medication-plans/{plan_id}/check-ins
    POST   /api/v1/homes/{home_id}/family-assistant
    POST   /api/v1/homes/{home_id}/livekit/token

El backend exige consentimientos específicos como audio_capture, video_capture,
medication_management y family_mode. Family mode y medicación están acotados
por hogar, sujeto, rol y consentimiento. Las dosis son estados administrativos
(pending, taken, skipped, missed).

### Ejecutar backend en Windows sin Docker

Desde la carpeta local `one`:

    py -3.12 -m venv .venv
    .\.venv\Scripts\Activate.ps1
    pip install -e ".[dev]"
    $env:ONE_DATABASE_URL = "sqlite:///./one.db"
    python -m uvicorn app.main:app --reload --port 8000

En otra terminal, para tests:

    cd C:\ruta\al\ONE\one
    .\.venv\Scripts\Activate.ps1
    $env:ONE_DATABASE_URL = "sqlite:///./one.db"
    pytest

No copiar el .env del PC actual. Usar .env.example como plantilla y rellenar
localmente solo los valores necesarios. No imprimir el contenido de .env en el
chat.

### Ejecutar con Docker

    cd C:\ruta\al\ONE\one
    docker compose up --build

Compose incluye API, PostgreSQL, Redis, MinIO, LiveKit de desarrollo y Caddy.
Los valores de desarrollo (devkey, secret, contraseñas de ejemplo) no son
válidos para compartir el servicio en una LAN o producción. Para teléfonos,
LiveKit necesita una URL alcanzable por el dispositivo y cámara/micrófono
requieren origen seguro HTTPS/WSS.

---

## 8. Frontend web (one-frontend)

Es un dashboard React/Vite para cuidadores y un publisher de cámara de
navegador. Tiene modo demo determinista y modo backend real:

    cd C:\ruta\al\ONE\one-frontend
    npm install
    Copy-Item .env.example .env -ErrorAction SilentlyContinue
    npm run dev

Comandos de verificación:

    npm run typecheck
    npm run lint
    npm test
    npm run build

En .env, VITE_DEMO_MODE=true permite revisar la UI con datos sintéticos. Para
backend real, usar VITE_DEMO_MODE=false y un VITE_API_BASE_URL apropiado. El
cliente tipado está en src/api/client.ts y el esquema generado en
src/api/schema.d.ts; el snapshot se basa en el contrato del backend. No alterar
el snapshot para ocultar un desacuerdo: si el backend cambia, debe existir una
decisión explícita y coordinada.

Rutas web relevantes: /dashboard, /dashboard/map, /dashboard/events,
/dashboard/assistant, /dashboard/family, /dashboard/privacy, /publisher y
/join/:code?.

---

## 9. iOS (one-ios) como referencia funcional

La app iOS es SwiftUI para iOS 26.0 y Xcode 26.2 o posterior. Contiene:

- caregiver overview;
- family care circle con roles de mínimo privilegio;
- recordatorios de medicación;
- mapa aproximado y RoomPlan/LiDAR;
- eventos y asistente de residente;
- controles de privacidad;
- Keychain/session abstractions;
- captura RoomPlan condicionada a dispositivo LiDAR.

El modelo normalizado de RoomPlan usa units = m y eje vertical Y. RoomPlan
requiere dispositivo físico compatible; simulador y dispositivos sin LiDAR usan
fallback de zonas manuales. iOS también mantiene el contrato API revisado en
contracts/openapi.json.

La lógica de cámara del MVP distingue el iPhone cuidador/residente de un
publisher de navegador. No asumir que Android deba copiar literalmente cada
detalle de iOS: replicar comportamiento y principios, adaptando patrones
nativos de Android.

Para la equiparación de Android realizada el 15-09-2026 se revisaron
`One/Features/Home/HomeView.swift`, `Family/FamilyView.swift`,
`Assistant/AssistantView.swift`, `Map/MapView.swift`,
`Settings/SettingsView.swift` y `One/Core/DesignSystem/OneTheme.swift`.
El shell de cuidador de iOS usa Home, Map, Family, Assistant y Account; Android
mantiene esa misma jerarquía y conserva Events como destino secundario.

---

## 10. Seguridad, privacidad y lenguaje de producto

No hacer afirmaciones clínicas. Usar expresiones como:

- observation / signal / with context and uncertainty;
- administrative medication reminder;
- consent-based;
- not a diagnosis;
- review with the person or care team.

Evitar:

- ONE knows what happened;
- the person is safe cuando solo existe una señal parcial;
- medical advice;
- camera viewing is local si se está mostrando vídeo LiveKit desde otro
  dispositivo;
- fechas ISO crudas, nombres técnicos de frames o etiquetas como event(s)
  cuando una frase natural sea posible.

La cámara, audio, clips, mapas, eventos y datos familiares deben tener una
explicación de consentimiento y alcance. El publisher debe poder detenerse. La
app debe limpiar sesión, estado sensible y tareas/notificaciones asociadas al
usuario cuando se cierra sesión.

---

## 11. Pendientes conocidos y áreas a revisar

Estos puntos provienen de la revisión de Android y deben tratarse como lista de
seguimiento, no como autorización automática para cambiar el backend:

1. Revisar avisos de versiones de dependencias Gradle sin actualizar a ciegas.
2. Añadir más pruebas Compose de navegación y accesibilidad.
3. Validar en dispositivo físico permisos de cámara, micrófono,
   notificaciones y LiveKit.
4. Probar login por email/pairing con backend real y expiración/revocación de
   sesión.
5. Probar creación de hogar, invitación, cambio de miembro y logout.
6. Verificar todos los estados de recordatorio en horarios locales y cambios
   de día/zona horaria.
7. Verificar que las alarmas no sobrevivan indebidamente a edición,
   desactivación o logout.
8. Comprobar backup del dispositivo y ausencia de datos sensibles no deseados.
9. Verificar textos visibles en todas las pantallas, incluidos estados vacíos,
   errores de red, publisher, cámara, mapas y permisos.
10. Verificar que las acciones activas de Home sigan abriendo sus destinos:
    care space/selector, plan de hoy/Family, Map, Cameras, Events y detalle de
    evento.
11. Confirmar que YOU aparece junto al nombre y que el email no reaparece en la
    vista de familia.
12. Revisar que todos los popup mantienen el estilo ONE en pantallas pequeñas,
    con teclado y con contenido largo.
13. Confirmar que release siempre recibe un endpoint HTTPS real.

Si una prueba falla por falta de emulador, backend, LM Studio, GPU, LiveKit o
permisos físicos, documentar el bloqueo exacto en vez de simular que pasó.

---

## 12. Flujo de trabajo recomendado para cada nueva tarea

1. Leer este archivo y el README del repo afectado.
2. Confirmar el repo afectado y ejecutar git status --short --branch.
3. Si es Android, inspeccionar primero OneApp.kt, OneAppState.kt, OneApi.kt y
   los modelos necesarios. Mantener el backend sin cambios y conservar la
   compatibilidad del adaptador con su contrato.
4. Identificar si el problema es de UI, estado local, navegación, permisos,
   caché o contrato. No solucionar un problema de UI editando la API.
5. Implementar el cambio mínimo y coherente con el diseño ONE.
6. Ejecutar pruebas/build/lint apropiados.
7. Revisar el diff con git diff y confirmar que no aparecen secretos ni cambios
   accidentales en otro repo.
8. Crear un commit descriptivo solo cuando el usuario lo pida o cuando forme
   parte del flujo de trabajo acordado.
9. Hacer push únicamente en el repo que se haya modificado.

Ejemplo seguro para Android después de modificar código:

    cd C:\ruta\al\ONE\one-android
    git status --short --branch
    .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
    git diff --check
    git diff --stat
    git add app CONTEXTO_ONE_PORTATIL.md
    git commit -m "describe the Android change"
    git push origin main

Si el usuario todavía no quiere publicar, detenerse después de verificar y
dejar el commit local o los cambios sin commit según haya pedido.

---

## 13. Flujo de actualización y publicación entre los dos PCs

Regla práctica: antes de empezar en un PC, hacer pull; antes de volver al otro,
hacer commit y push; al regresar al primer PC, hacer otro pull.

Ejemplo para cambios Android hechos en el portátil:

    cd C:\ruta\al\ONE\one-android
    git pull --ff-only origin main
    # trabajar y probar
    git status --short --branch
    git add app CONTEXTO_ONE_PORTATIL.md
    git commit -m "describe the change"
    git push origin main

Después, en el PC principal:

    cd C:\ruta\al\ONE\one-android
    git pull --ff-only origin main

Para cambios de backend, frontend, iOS o docs se repite el mismo patrón,
entrando en su subcarpeta correspondiente. Nunca hacer git add . desde la raíz
ONE, porque la raíz no es el repo y puede mezclar archivos temporales o
intentar incluir datos de trabajo que no deben publicarse.

Si Git informa de ramas divergentes o conflictos, no ejecutar reset. Guardar el
estado, revisar el conflicto y pedir instrucciones si el resultado no es obvio.

---

## 14. Mensaje inicial sugerido para el siguiente chat de Codex

Se puede pegar el siguiente texto junto con este archivo:

Lee CONTEXTO_ONE_PORTATIL.md completo antes de trabajar. Estoy continuando ONE
Cognitive Companion desde one-android. Respeta estrictamente que el backend
es solo lectura porque lo mantiene otro compañero. Puedes modificar el
adaptador Android OneApi.kt y sus llamadas si la tarea lo requiere, pero no
cambies los contratos del backend sin coordinación. Primero
comprueba git status, inspecciona el código relevante, implementa solo lo
solicitado y verifica con Gradle/lint/tests. No expongas secretos ni valores de
.env. Si hay una discrepancia entre el contexto, el README y el código, trata
el código y el contrato versionado como evidencia y explícame la discrepancia
antes de ampliar el alcance.

---

## 15. Checklist de llegada al portátil

- [ ] Si este contexto todavía no se ha publicado, copiarlo a la raíz del
      nuevo ONE; si ya se publicó dentro de `one-android`, recuperarlo con el
      pull de ese repo.
- [ ] Confirmar que los cinco repos están clonados con los remotos indicados:
      `one-android`, `one`, `one-frontend`, `one-ios` y `one-docs`.
- [ ] Ejecutar git pull --ff-only origin main dentro de cada repo.
- [ ] Abrir one-android en Android Studio y sincronizar Gradle.
- [ ] Configurar un emulador/dispositivo y permisos necesarios.
- [ ] Ejecutar testDebugUnitTest, lintDebug y assembleDebug.
- [ ] Arrancar backend local si se prueba contra API real.
- [ ] Usar demo mode solo para revisar la UI sin backend.
- [ ] No copiar .env, local.properties, keystores ni datos de usuario.
- [ ] Antes de publicar, revisar git diff, git status y el repo exacto.

Fin del contexto de traspaso.

