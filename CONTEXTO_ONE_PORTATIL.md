# CONTEXTO DE TRASPASO — ONE Cognitive Companion

> Documento preparado para continuar el desarrollo de ONE desde otro PC.
> Leerlo completo antes de modificar código. Este archivo es un contexto de
> trabajo, no sustituye a los README ni al contrato OpenAPI versionado.

Fecha de actualización: 2026-09-22 (revisión del estado Android, Docker y documentación)
Proyecto local de Codex: Hackathon
Carpeta contenedora de referencia en este PC: C:\Users\alcar\Desktop\Development\ONE
Producto: ONE Cognitive Companion
Estado general: MVP multi-cliente local-first, con backend FastAPI, cliente
iOS nativo, cliente Android nativo, dashboard web y documentación Vocs.
Estado de esta sesión: el trabajo activo está separado entre la rama
`feature/outside-companion-tracking` de `one-android` y la rama `android-test`
del backend local `one-backend`. El tracker exterior conserva el mapa estilo
Life360 y sincroniza ubicación, dispositivos y lugares seguros con el backend
cuando hay consentimiento. Se ajustaron además el selector de personas, el
seguimiento de cámara al mover el mapa y el check-in diario. Consulta la
sección 16 para el estado exacto de este PC, cambios locales, Docker y comandos
para reproducir el entorno. Este contexto debe actualizarse en cada traspaso
entre máquinas.

---

## 1. Instrucciones prioritarias para el siguiente agente

Estas reglas son obligatorias salvo que el usuario las cambie expresamente:

1. El usuario autorizó expresamente el 22-09-2026 los cambios coordinados de
   Android y backend para las pruebas de ubicación. Mantener Android aislado en
   `feature/outside-companion-tracking` y el backend en `android-test`; no
   mezclarlos todavía en `main` ni en `demo-release`.
2. Todo cambio posterior de contrato debe actualizar conjuntamente backend,
   `contracts/openapi.json`, tests y `OneApi.kt`. No usar cambios de API para
   ocultar una incidencia puramente visual.
3. El análisis interior por cámara permanece en `feature/camera-room-analysis`
   y no debe mezclarse con el trabajo actual de seguimiento exterior.
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

La prioridad actual es validar Android contra el backend Docker real. El
backend es la fuente de verdad del contrato; iOS y frontend sirven como
referencia funcional y visual.

---

## 2. Estructura real del proyecto y Git

La carpeta ONE es una carpeta contenedora y no es un repositorio Git. No se debe
ejecutar git pull en ONE. Cada subcarpeta principal es un repo independiente.
En este PC el checkout del repositorio backend está en `one-backend` (en otros
equipos también puede llamarse `one`):

- one-android: app Android Kotlin/Compose
  Remoto: https://github.com/maualc/one-android
- one / one-backend: API FastAPI y servicios locales
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

### Estado observado el 22-09-2026

`one-android/main` queda reservado para versiones estables. El trabajo activo
de seguimiento debe permanecer en `feature/outside-companion-tracking` en
Android y `android-test` en backend; no mezclarlo con `main`,
`feature/demo-release` ni `feature/camera-room-analysis`.

Commits relevantes observados:

- one-android — punto de partida `4316d7c Refresh portable handoff context`
  en `feature/outside-companion-tracking`, igual que `origin` al comenzar esta
  sesión; consultar `git log` y `git status` para saber si ya se creó/publicó
  el commit de los cambios descritos en la sección 16.
- one-backend — `7f66672 Add backend support for outside location tracking` en
  `android-test`; README y `.env.example` modificados y `docs/docker.md` nuevo,
  pendientes de commit.
- one-docs — `77261f2 Document care analytics and safety context` en `main`;
  README y tres guías Docker/quickstart/troubleshooting modificadas localmente.
- one-frontend — `2308e1d Add daily check-in and safety analytics flows`,
  `main` limpio.
- one-ios — `cc37ebc Add daily check-in and safety review surfaces`, `main`
  limpio.

Hay una carpeta adicional `one-espionage` que no forma parte de los cinco
repositorios del proyecto: su rama `main` está 8 commits atrasada y tiene
cambios/borrados locales. No tocarla, actualizarla, limpiarla ni incluirla en
commits de ONE sin una petición explícita.

En el momento de esta revisión, los contenedores del proyecto existían pero
estaban parados (`Exited (0)`); no asumir que Docker sigue levantado. Antes de
la parada, `/api/v1/health` devolvía estado `degraded` con PostgreSQL en error y
los logs de API mostraban `psycopg.OperationalError: the connection is closed`
tras reiniciarse PostgreSQL. Se documenta en la sección 16 cómo levantar y
verificar el stack.

Commits Android recientes publicados:

- `73e74c3 fix: allow local backend in debug builds`: permite HTTP solo en
  builds debug mediante `network_security_config`; release sigue requiriendo
  HTTPS.
- `12e512a fix: guide publisher room capture`: el publisher abre la captura al
  recibir un trabajo y muestra progreso/indicaciones para los 20 frames.
- `cc7221f fix: hide unusable room maps`: oculta revisiones legacy, fallidas,
  rechazadas o sin geometría; añade desplazamiento/zoom del mapa y evita que
  el refresco muestre “Loading…” indefinidamente cuando falla la API.

El archivo está dentro de `one-android` y versionado por ese repositorio. La
carpeta ONE contenedora sigue sin ser un repositorio y nunca se debe hacer
`git add .` desde ella.

---

## 3. Cómo preparar el portátil

### Opción A: los repos ya están clonados

Abrir PowerShell en la carpeta contenedora y revisar primero cada árbol. Para
continuar el tracker, actualizar las ramas de trabajo correctas (no hacer pull
de `main` en Android/backend):

    cd C:\ruta\al\ONE
    git -C one-android status --short --branch
    git -C one-android switch feature/outside-companion-tracking
    git -C one-android pull --ff-only origin feature/outside-companion-tracking
    git -C one-backend status --short --branch
    git -C one-backend switch android-test
    git -C one-backend pull --ff-only origin android-test

Para los repos de referencia, usar `main` solo si se van a consultar o
actualizar deliberadamente:

    git -C one-frontend pull --ff-only origin main
    git -C one-ios pull --ff-only origin main
    git -C one-docs pull --ff-only origin main

Si algún repo tiene cambios locales, detenerse y revisarlos antes del pull; no
sobrescribirlos ni cambiar de rama si Git indica que no es seguro.

### Opción B: clonar desde cero

    mkdir C:\ruta\al\ONE
    cd C:\ruta\al\ONE
    git clone --branch feature/outside-companion-tracking https://github.com/maualc/one-android.git one-android
    git clone --branch android-test https://github.com/0xbiel/one.git one-backend
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
- La ubicación exterior siempre muestra que es aproximada y distingue GPS de
  simulación; una zona no equivale a una coordenada exacta.
- La vista del cuidador puede revisar hogar, cámaras, mapa exterior, eventos, clips,
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
  medicación, familia, eventos, notificaciones, logout y caché. El estado del
  tracker exterior vive en `OneOutsideTrackingStore` para no mezclarlo con la
  caché del backend.
- OneApi.kt: modelos, sesiones y cliente HTTP del contrato. Debe conservarse
  sin cambios de rutas, cuerpos ni semántica; no tocarlo para arreglar una
  incidencia puramente visual o de conectividad local.
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
- OneOutsideTrackingUi.kt: resumen cuadrado con avatar, mapa detallado,
  selección de rango, zonas, historial, alertas, demo de rutas y mensajes
  locales de prueba.
- OneOutsideTrackingStore.kt: SQLite local con perfiles/fotos, lugares, puntos
  con calle y duración de estancia, y alertas; conserva como máximo siete días.
- OneOutsideLocationService.kt: servicio foreground de ubicación, geofences y
  notificaciones Android; inicia la muestra GPS y resuelve calles para puntos
  fuera de zonas o estancias largas.
- OneOutsideLocationSearch.kt: búsqueda y reverse geocoding con Nominatim/OSM.
- OneExteriorMap.kt y OneExteriorCompanionModels.kt: MapLibre/OSM y geometría
  geográfica de soporte.
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

La navegación de `main` sigue alineada con el shell de iOS y contiene cinco
áreas en la barra inferior: Home, Map, Family, Assistant y Account. En
`feature/outside-companion-tracking`, Map es exclusivamente el tracker exterior
local; no se muestran escaneo, calibración ni generación de mapas interiores.
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
- Map: el canvas 2D y la calidad del mapa aparecen antes que las herramientas;
  solo una revisión `ready` con geometría real se presenta como activa. El
  canvas válido admite arrastre y zoom con gesto de pinza.
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
La equiparación visual original no modificó el contrato. La rama Android del
tracker amplía ahora `OneApi.kt` de forma coordinada con el backend `android-test`.

El enrollment facial Android permite escoger cámara frontal o trasera, reinicia
las muestras al cambiar de cámara, valida tres vistas con ML Kit y envía el
`camera_position` correcto. La UI refleja que el reconocimiento es opcional,
local al care space y controlado por cuidadores; las fotos no se conservan como
fotos después de derivar las plantillas.

La tarjeta de personas cuidadas también permite activar o desactivar desde
Android los recordatorios de medicación por persona. Usa el consentimiento
existente `medication_management`, bloquea el interruptor mientras se actualiza
y muestra el error junto al control.

### Flujos conectados en Android + backend `android-test`

- Family distingue la cuenta del hogar de la persona cuidada seleccionada.
- Consentimientos, medicación, check-in diario y asistente familiar se dirigen
  al `care_recipient_id` correcto.
- La tarjeta de contexto muestra analítica agregada de 30 días sin presentar
  señales como diagnósticos.
- Los detalles de eventos pueden descargar y mostrar su captura privada usando
  la sesión autenticada.
- El mapa exterior mezcla caché local y datos remotos, conserva IDs
  idempotentes y expone el error de sincronización sin bloquear el GPS local.

### Correcciones recientes ya publicadas en Android

Además de la equiparación visual histórica (`50ed0da` y anteriores), `main`
contiene actualmente:

- `2cf4629`: mapeado 2D de habitaciones a partir de recorridos de cámara,
  generación, calibración, evidencias, importación y mapas manuales.
- `73e74c3`: HTTP permitido únicamente en debug para el backend LAN local.
- `12e512a`: el publisher recibe el trabajo, solicita cámara automáticamente y
  guía la captura de hasta 20 frames.
- `cc7221f`: validación de mapas utilizables, ocultación de revisiones inválidas,
  gestos de movimiento/zoom y polling que no machaca los errores de API.

La revisión final comunicada para `50ed0da` indicó:

- testDebugUnitTest, assembleDebug, lintDebug y assembleRelease OK.
- Tests instrumentados en Pixel 9 API 36 OK.
- Lint sin errores; quedaban únicamente avisos de versiones/dependencias.
- Backend sin cambios.
- Se añadieron/ajustaron vuelta atrás, logout de publisher, reconciliación de
  alarmas, protección de caché/backup, splash, tarjetas, pestañas, demo y
  textos.
- `one-backend` sigue sin cambios. La rama del tracker solo conserva el
  cliente API heredado como compatibilidad; no añade rutas ni datos de
  ubicación remotos.

No asumir que build correcta significa que todos los flujos físicos estén
verificados: permisos, cámara/WebRTC, LiveKit, notificaciones y dispositivos
reales siguen dependiendo del entorno.

### Tracker exterior en `feature/outside-companion-tracking`

La pestaña `Map` de esta rama ya no carga mapas de habitaciones. El flujo local
es:

1. Seleccionar la persona cuidada; en demo offline aparece un perfil local y
   con backend se usan los care recipients. Cuenta/hogar y destinatario de
   cuidados son selecciones independientes.
2. La vista inicial es un mapa cartográfico cuadrado con la foto del perfil (o
   iniciales) anclada a la coordenada del punto actual, no al centro de la pantalla,
   y círculos por cada punto reciente. El usuario puede desplazar el mapa y
   volver a centrarlo con el botón de ubicación. La foto se puede
   elegir desde el perfil local; si el API entrega `profile_photo_url` o
   `photo_url`, se usa como fallback. El mapa cartográfico detallado se abre
   desde `History` o `Configure map`.
3. Configurar `Home` y lugares seguros tocando el mapa detallado o usando la
   última posición fiable del teléfono. Ambos radios parten de 20 m y se pueden
   adaptar desde la interfaz, con un mínimo configurable de 20 m.
4. Activar `Location sharing on this phone`. Android usa un foreground service
   de GPS con muestras cada 20 s mientras hay movimiento y cada 90 s en reposo,
   una última ubicación solo si es reciente y geofences para avisos de
   entrada/salida. Tras el permiso de uso de la app, la interfaz explica y pide
   por separado el acceso en segundo plano; en Android 11+ se abre Ajustes para
   seleccionar `Allow all the time`. Si se deniega, queda el seguimiento en
   primer plano con un aviso y una vía para volver a habilitarlo.
5. Cuando un punto fiable queda fuera de las zonas o acumula una estancia de al
   menos cinco minutos, el servicio intenta resolver y guardar el nombre de la
   calle. La estancia se conserva con su duración y se muestra en el historial,
   sin usar `Outside configured zones` como ubicación del historial.
6. En el historial se puede elegir 1 hora, 6 horas, 24 horas, 3 días o 7 días;
   siete días es el máximo local. Los puntos GPS se muestran como observaciones,
   sin unirlos con rectas que aparenten un trayecto por calles no comprobado.
   La reconstrucción vial requiere un motor de map matching y una política de
   privacidad antes de enviar trazas a un proveedor. `Test a route` solo aparece en demo mode y
   permite probar salida, llegada a un lugar seguro y vuelta a casa sin moverse.
7. Con consentimiento `outside_location`, el móvil registra un dispositivo
   estable por destinatario, sube puntos en lotes idempotentes, descarga hasta
   siete días de historial y sincroniza altas, cambios y bajas de lugares
   seguros. Al detener el GPS, el dispositivo remoto pasa a `paused`.
8. `Resident message preview` sigue creando una notificación en el propio
   teléfono; los mensajes entre teléfonos aún no se sincronizan.

La precisión de cada punto se conserva y la app no clasifica zonas cuando el
radio de error supera 120 m. El historial, radios, alertas y mensajes de esta
primera entrega se guardan primero en Android y se replican en backend cuando
hay sesión y consentimiento; no deben interpretarse como un servicio de
emergencia ni como una coordenada exacta.

El APK de debug local para esta rama se genera con
`./gradlew :app:assembleDebug` en `app/build/outputs/apk/debug/app-debug.apk`.
En verificaciones anteriores se ejecutaron tests unitarios e instrumentados en
emulador API 36. En la sesión más reciente, `:app:assembleDebug` terminó con
`BUILD SUCCESSFUL`; no se pudo verificar la ejecución en emulador porque `adb`
no está disponible en el PATH de esta terminal. Queda pendiente validar el
seguimiento real en un teléfono físico y la concesión del permiso de segundo
plano en cada versión de Android. El mapa y la resolución del nombre de calle
requieren red; la consulta de calle puede enviar coordenadas al geocodificador.

### Conectividad LAN para probar en un teléfono físico

El APK de debug normal usa `10.0.2.2` para el emulador. En una prueba anterior
se usó el endpoint LAN `http://192.168.1.134:8000/api/v1`; esa IP puede haber
cambiado y no debe reutilizarse sin comprobar `ipconfig`:

    .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug `
      -PoneApiBaseUrl=http://192.168.1.134:8000/api/v1

Artefacto actual (no se versiona en Git):

    app/build/outputs/apk/debug/one-debug-lan-192.168.1.134.apk

El backend Compose debe estar expuesto a la LAN para ese test. La configuración
local `.env` y cualquier override de Compose son específicos del PC y están
ignorados; no copiarlos ni añadirlos al contexto. El teléfono y el PC deben
estar en la misma Wi‑Fi. Comprobar desde el navegador del teléfono:

    http://192.168.1.134:8000/api/v1/health

Si no responde, revisar que Docker esté arrancado y el firewall de Windows
permita la red privada. `OneApi.kt` transforma cualquier excepción de la
petición en el texto genérico `Could not reach the ONE API`; ese mensaje por sí
solo no identifica si falló DNS, socket, timeout o la respuesta. En la
configuración inspeccionada, el recurso debug ya permite HTTP y el recurso main
(usado por release) lo prohíbe; por tanto no atribuir el error al bloqueo HTTP
sin más evidencia. Revisar endpoint compilado, reachability y Logcat. LiveKit
también necesita anunciar una URL accesible por el teléfono; `localhost` solo
sirve dentro del propio PC.

---

## 6. Android: configuración y comandos

### Endpoint de API

El valor por defecto de debug está pensado para el emulador:

    http://10.0.2.2:8000/api/v1

`app/src/debug/res/xml/network_security_config.xml` permite HTTP en debug para
el backend local; `app/src/main/res/xml/network_security_config.xml` mantiene
HTTP deshabilitado, por lo que release debe usar HTTPS. En móvil físico,
compilar con la IP actual del PC (no una IP antigua):

    .\gradlew.bat :app:assembleDebug -PoneApiBaseUrl=http://<IP_DEL_PC>:8000/api/v1

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

Una verificación anterior en este PC (22-09-2026) ejecutó:

    $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio1\jbr'
    .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug `
      -PoneApiBaseUrl=http://192.168.1.134:8000/api/v1

En esa verificación, `testDebugUnitTest`, `assembleDebug` y `lintDebug`
terminaron con `BUILD SUCCESSFUL`; lint no produjo errores. El backend
`android-test` pasó `uv run --extra dev pytest -q` con 2 omisiones esperadas.
La comprobación actual de Docker y la build más reciente están descritas en la
sección 16. El test instrumentado depende de que haya un dispositivo conectado.
Si Android Studio muestra “Project
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

El caché puede contener eventos, cámaras, recordatorios y, en la rama del
tracker, hasta siete días de ubicación local. Mantenerlo acotado, apagar el
servicio al cerrar sesión y revisar las reglas de backup. No activar copias de
seguridad de datos sensibles por defecto. El vídeo debe permanecer transitorio
según el diseño y los avisos de la UI deben reflejar si la cámara es local o
una vista LiveKit remota.

---

## 7. Backend (carpeta local `one` o `one-backend`)

El backend es un monolito modular FastAPI para el MVP local-first. Usa
PostgreSQL como base autoritativa en Compose y SQLite como fallback de cero
configuración para desarrollo/tests.

### Integración añadida en `android-test` (22-09-2026)

- Migración `016_outside_location_tracking.sql` con dispositivos, puntos de
  ubicación y lugares seguros.
- Rutas consentidas `outside_location` para registrar/pausar dispositivos,
  subir lotes idempotentes, consultar última posición/historial de siete días
  y crear, editar o eliminar lugares seguros.
- El contrato `contracts/openapi.json` se regeneró tras añadir las rutas.
- Tests de consentimiento, idempotencia, límites temporales y contrato
  PostgreSQL.
- Compose arranca por defecto el núcleo (API, PostgreSQL, Redis, MinIO y
  LiveKit). El worker de visión es opcional con el perfil `vision`; web y demo
  usan perfiles separados.
- Guía de pruebas en `docs/android-testing.md` y descarga de modelos faciales
  mediante `scripts/download_face_models.ps1`. Los modelos y `.env` locales no
  se versionan.

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
    POST   /api/v1/homes/{home_id}/care-recipients/{recipient_id}/tracking-devices
    POST   /api/v1/homes/{home_id}/care-recipients/{recipient_id}/location-points
    GET    /api/v1/homes/{home_id}/care-recipients/{recipient_id}/locations
    GET    /api/v1/homes/{home_id}/care-recipients/{recipient_id}/safe-places
    GET    /api/v1/homes/{home_id}/maps/current
    POST   /api/v1/homes/{home_id}/cameras/{camera_id}/map-generation
    GET    /api/v1/homes/{home_id}/cameras/{camera_id}/map-generation
    POST   /api/v1/homes/{home_id}/cameras/{camera_id}/map-generation/{job_id}/frames
    POST   /api/v1/homes/{home_id}/livekit/token

El backend exige consentimientos específicos como audio_capture, video_capture,
medication_management y family_mode. Family mode y medicación están acotados
por hogar, sujeto, rol y consentimiento. Las dosis son estados administrativos
(pending, taken, skipped, missed).

### Ejecutar backend en Windows sin Docker

Desde la carpeta local `one-backend`:

    py -3.12 -m venv .venv
    .\.venv\Scripts\Activate.ps1
    pip install -e ".[dev]"
    $env:ONE_DATABASE_URL = "sqlite:///./one.db"
    python -m uvicorn app.main:app --reload --port 8000

En otra terminal, para tests:

    cd C:\ruta\al\ONE\one-backend
    .\.venv\Scripts\Activate.ps1
    $env:ONE_DATABASE_URL = "sqlite:///./one.db"
    pytest

No copiar el .env del PC actual. Usar .env.example como plantilla y rellenar
localmente solo los valores necesarios. No imprimir el contenido de .env en el
chat.

### Ejecutar con Docker

    cd C:\ruta\al\ONE\one-backend
    if (-not (Test-Path .env)) { Copy-Item .env.example .env }
    docker compose up --build -d
    docker compose ps
    Invoke-WebRequest -Uri "http://127.0.0.1:8000/api/v1/health" -UseBasicParsing

El stack por defecto levanta API, PostgreSQL 16, Redis 7, MinIO y LiveKit local.
El perfil `--profile web` añade el frontend sibling y Caddy; el frontend se abre
en `http://127.0.0.1:4175`. El perfil opcional `--profile vision` añade el
worker de geometría (necesita pesos/modelos locales); `--profile demo` necesita
el repo sibling `one-demo-service`, que no está en esta carpeta de proyecto.
Las migraciones PostgreSQL, incluida `016_outside_location_tracking.sql`, se
aplican durante el arranque de la API. Los puertos por defecto son loopback:
API 8000, frontend 4175; con el `.env.example` actual Caddy usa HTTPS 8443 y
HTTP 8080. El Compose fallback sin `.env` usa 8080/8081 para Caddy.
Los valores de desarrollo (devkey, secret, contraseñas de ejemplo) no son
válidos para compartir el servicio en una LAN o producción. Para teléfonos,
LiveKit necesita una URL alcanzable por el dispositivo y cámara/micrófono
requieren origen seguro HTTPS/WSS. No guardar ni copiar `.env` a Git.

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
    git commit -m "describe the change"
    git push origin feature/outside-companion-tracking

Si el usuario todavía no quiere publicar, detenerse después de verificar y
dejar el commit local o los cambios sin commit según haya pedido.

---

## 13. Flujo de actualización y publicación entre los dos PCs

Regla práctica: antes de empezar en un PC, hacer pull; antes de volver al otro,
hacer commit y push; al regresar al primer PC, hacer otro pull.

Ejemplo para cambios Android hechos en el portátil:

    cd C:\ruta\al\ONE\one-android
    git switch feature/outside-companion-tracking
    git pull --ff-only origin feature/outside-companion-tracking
    # trabajar y probar
    git status --short --branch
    git add app CONTEXTO_ONE_PORTATIL.md
    git commit -m "describe the change"
    git push origin feature/outside-companion-tracking

Después, en el PC principal:

    cd C:\ruta\al\ONE\one-android
    git switch feature/outside-companion-tracking
    git pull --ff-only origin feature/outside-companion-tracking

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
Cognitive Companion desde one-android en
`feature/outside-companion-tracking`, coordinada con `one-backend` en
`android-test`. Comprueba el estado de cada repo antes de editarlo; no mezcles
ramas y no modifiques contratos de API salvo que la tarea lo requiera.
Primero
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
      `one-android`, `one-backend`, `one-frontend`, `one-ios` y `one-docs`.
- [ ] Usar `feature/outside-companion-tracking` en one-android y `android-test`
      en one-backend; los repos de referencia siguen en `main`.
- [ ] Abrir one-android en Android Studio y sincronizar Gradle.
- [ ] Configurar un emulador/dispositivo y permisos necesarios.
- [ ] Ejecutar testDebugUnitTest, lintDebug y assembleDebug.
- [ ] Arrancar backend local si se prueba contra API real.
- [ ] Usar demo mode solo para revisar la UI sin backend.
- [ ] No copiar .env, local.properties, keystores ni datos de usuario.
- [ ] Antes de publicar, revisar git diff, git status y el repo exacto.

---

## 16. Estado del PC y guía de reproducción (22-09-2026)

Esta sección es la referencia más reciente para el trabajo actual. Los estados
de contenedores y los cambios locales pueden variar; confirmarlos con los
comandos antes de continuar.

### Cambios Android de esta sesión

En `feature/outside-companion-tracking`, sobre `4316d7c`:

- Family usa como selección principal a la persona cuidada, evita duplicar el
  selector de miembro del hogar y reduce el texto explicativo redundante.
- Map comparte la selección de care recipient con Family. Si se arrastra el
  mapa se desactiva el seguimiento automático de cámara; cambiar de persona o
  pulsar recenter lo vuelve a activar. Se simplificó el estado de ubicación.
- La hoja del check-in se abre directamente expandida, usa el color de canvas
  de la app y mantiene sus respuestas si falla el envío, con acciones de
  reintento y revisión.
- No se cambió el contrato ni se editó el backend en esta sesión.

Verificación más reciente: `.\gradlew.bat :app:assembleDebug` terminó
correctamente. APK generado en
`app/build/outputs/apk/debug/app-debug.apk`. No se instaló ni se verificó en un
emulador en esa pasada: `adb` no está disponible en el PATH de esta terminal.

**Diagnóstico de conexión corregido:** `OneApi.kt` transforma excepciones de la
petición en `Could not reach the ONE API`, ocultando la causa concreta. El
recurso debug de Network Security ya permite HTTP (`10.0.2.2` o IP LAN); el
recurso principal/release exige HTTPS. La explicación anterior que atribuía el
error al bloqueo HTTP fue incorrecta y no debe repetirse. Hay que comprobar la
URL incluida en BuildConfig, la conectividad y Logcat.

### Qué contiene Docker en `one-backend`

El Dockerfile raíz construye la API con Python 3.12 e instala las dependencias
de runtime de `.[postgres]`; copia la app y las migraciones SQL. Al iniciar, la
API aplica las migraciones numeradas, incluida
`016_outside_location_tracking.sql`. El Compose normal levanta:

| Servicio | Función | Puerto publicado por defecto |
| --- | --- | --- |
| `api` | FastAPI de ONE | `127.0.0.1:8000` |
| `postgres` | PostgreSQL 16 y almacenamiento autoritativo | interno |
| `redis` | servicio de caché/coordinación | interno |
| `minio` | almacenamiento compatible con S3 | interno |
| `livekit` | WebRTC local de desarrollo, sin LiveKit Cloud | `7880`, `7881`, `7882/udp` en todas las interfaces |
| `caddy` (`web`) | reverse proxy HTTPS/HTTP local | IPv4 `8443`/`8080`; además IPv6 wildcard |

Perfiles opcionales:

- `web`: construye el repo sibling `one-frontend` y arranca frontend y Caddy.
  Con el `.env.example` actual, frontend `127.0.0.1:4175`, Caddy HTTPS
  IPv4 `8443` y HTTP IPv4 `8080`, más mappings IPv6 wildcard.
- `vision`: construye `geometry_service/Dockerfile`, un worker Python separado
  y pesado que requiere checkpoints/modelos locales montados; no hace falta
  para el tracker exterior ni para el check-in.
- `demo`: necesita `one-demo-service` junto a `one-backend`; ese checkout no
  estaba presente entre los cinco repos principales de este PC.

El stack no queda completamente limitado a loopback: API y frontend usan
127.0.0.1 por defecto, pero LiveKit publica sus puertos en todas las
interfaces y el servicio Caddy añade mappings IPv6 wildcard (`[::]`) aunque su
binding IPv4 sea loopback. `ONE_CADDY_BIND` solo controla el mapping IPv4.
Restringir LiveKit/Caddy con Windows Firewall en redes no confiables; quitar
la publicación IPv6 requiere cambiar explícitamente los mappings de Compose.

Los datos se conservan en volúmenes Docker con nombre (`one_pg`, `one_objects`,
`one_minio`, `caddy_data`, `caddy_config`). `docker compose down` conserva los
volúmenes; `docker compose down -v` los borra. Las imágenes, código, modelos y
configuración no contienen los datos de otro PC. La base actual puede contener
información personal y ubicación: no exportarla ni subirla a Git. Para tener
los mismos registros en otro PC se necesita un backup PostgreSQL trasladado de
forma privada y cifrada; levantar Compose desde cero crea una base nueva.

El `.env` es local/ignorado. Si no existe, crear una copia de la plantilla sin
sobrescribir una configuración existente:

~~~powershell
Set-Location C:\Users\alcar\Desktop\Development\ONE\one-backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
~~~

No copiar el `.env` real entre equipos ni mostrar sus contraseñas, secretos o
claves. En la última inspección de este PC los puertos de API no estaban
sobrescritos y Compose usaba el loopback por defecto (`127.0.0.1:8000`); el
frontend usaba `127.0.0.1:4175`, Caddy `8443/8080`. El `.env` local tenía
`ONE_GEOMETRY_REQUIRE_GPU=true` y apuntaba la geometría a `127.0.0.1:8090`.
Dentro del contenedor `127.0.0.1` es el propio contenedor; para acceder a un
worker ejecutado en Windows host, la dirección normalmente debe ser
`http://host.docker.internal:8090`. Esto solo afecta a geometría de mapas
interiores, no al tracker exterior.

### Arranque, verificación y frontend

Para reproducir el API y sus dependencias desde PowerShell:

~~~powershell
Set-Location C:\Users\alcar\Desktop\Development\ONE\one-backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build -d
docker compose ps
Invoke-WebRequest -Uri 'http://127.0.0.1:8000/api/v1/health' -UseBasicParsing
~~~

Para abrir también la web:

~~~powershell
docker compose --profile web up --build -d
Start-Process 'http://127.0.0.1:4175'
~~~

La salida saludable debe indicar `database_status: ok`. Si la base se reinició
por separado y la API quedó con una conexión psycopg cerrada, revisar logs y
reiniciar solo la API:

~~~powershell
docker compose logs --tail=150 api
docker compose logs --tail=150 postgres
docker compose restart api
Invoke-WebRequest -Uri 'http://127.0.0.1:8000/api/v1/health' -UseBasicParsing
~~~

En la última observación antes de apagar los servicios, el health devolvía
`status: degraded` / `database_status: error`, con `psycopg.OperationalError:
the connection is closed` en los logs. En la inspección posterior todos los
contenedores figuraban `Exited (0)`, por lo que el stack debe volver a arrancar
antes de probar la app.

`docker compose stop` detiene sin borrar contenedores ni volúmenes. Para
actualizar imágenes o aplicar cambios de Compose, repetir `docker compose up
--build -d`; un simple `restart` no aplica cambios de puertos ni variables.

### Android Studio y acceso desde dispositivos

Abrir la carpeta `one-android` directamente en Android Studio y dejar que
Gradle sincronice. Para el emulador, la URL por defecto del build debug es
`http://10.0.2.2:8000/api/v1`; `10.0.2.2` es el alias del host desde el
emulador, no desde un móvil físico. Ejecutar:

~~~powershell
Set-Location C:\Users\alcar\Desktop\Development\ONE\one-android
git switch feature/outside-companion-tracking
.\gradlew.bat :app:assembleDebug
~~~

Para teléfono físico, obtener la IPv4 actual del PC con `ipconfig`, editar el
`.env` local del backend para que `ONE_API_BIND=0.0.0.0`, levantar/recrear la
API (`docker compose up -d --force-recreate api`), permitir TCP 8000 solo en
la red privada de Windows Firewall y compilar Android con:

~~~powershell
.\gradlew.bat :app:assembleDebug -PoneApiBaseUrl=http://<IP_DEL_PC>:8000/api/v1
~~~

El teléfono y el PC deben estar en la misma Wi-Fi. Probar primero
`http://<IP_DEL_PC>:8000/api/v1/health` desde el navegador del teléfono. Para
release, usar siempre un endpoint HTTPS real.

### Repos y publicación

Los cambios Android y este contexto se publican solo en
`one-android:feature/outside-companion-tracking`. Los cambios locales de
`one-backend` (README, `.env.example` y `docs/docker.md` nuevo) y `one-docs`
(README, Docker, quickstart y troubleshooting) viven en repos y ramas separados;
no se incluyen en el push de Android. Revisar sus propios estados antes de
prepararlos para publicar. `npm` no está en el PATH de esta terminal, pero
`pnpm run build` de one-docs terminó correctamente usando el runtime Node/pnpm
disponible. No usar `git add .` desde la carpeta contenedora ONE.

Fin del contexto de traspaso.

