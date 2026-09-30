# ONE Android

La app Android usa Kotlin y Jetpack Compose, con CameraX para capturas
muestreadas y LiveKit para publicación en tiempo real. El backend procesa los
frames en memoria; la app no guarda vídeo localmente.

## Desarrollo

1. Arranca el backend (`one-backend`) en `http://127.0.0.1:8000`.
2. Abre este proyecto en Android Studio y ejecuta el emulador Pixel.
3. El emulador usa `http://10.0.2.2:8000/api/v1` si Docker publica la API en el loopback del PC.
4. Si `ONE_API_BIND` apunta solo a la IP LAN del PC, añade `one.apiBaseUrl=http://<IP_DEL_PC>:8000/api/v1` al `local.properties` ignorado. Android Studio usará esa URL al compilar debug; el emulador y el móvil deberán poder alcanzar esa IP.
5. Para una compilación puntual, `gradlew.bat assembleDebug -PoneApiBaseUrl=https://dev.example/api/v1` tiene prioridad sobre `local.properties`.

La captura requiere consentimiento `video_capture`, permiso de cámara y una
cámara habilitada en el hogar. LiveKit requiere además micrófono y una sesión
de dispositivo con rol `publisher`; una sesión normal de cuidador solo puede
suscribirse, según el contrato del backend.

## Producción

El endpoint debe ser HTTPS y se inyecta sin secretos:

```powershell
.\gradlew.bat bundleRelease -PoneApiBaseUrl=https://one.example/api/v1
```

Configura la firma de Play/App Signing en `gradle.properties` local o en los
secretos del CI; no se incluye ninguna clave en el repositorio. Antes de
publicar, valida consentimiento, política de privacidad, expiración de clips,
notificaciones y el servidor LiveKit desplegado.

## Planificación de cuidados

Las notas y citas se comparten mediante la API de `one-backend` con el
consentimiento `care_planning` de la persona cuidada. No se confunden con eventos
detectados automáticamente. Los avisos de citas se programan en el móvil tras
sincronizar; requieren permiso de notificaciones en Android 13+ y no sustituyen
un servicio de avisos remotos. Los borradores de formularios se cifran en el
dispositivo, se separan por hogar/persona y se eliminan al cerrar sesión.

Para probar esta pantalla con Docker, actualiza primero el backend hasta la
migración `018_care_planning.sql` y reconstruye su API. En un móvil físico,
configura `one.apiBaseUrl` con una URL del PC alcanzable desde el móvil; la
dirección `10.0.2.2` solo sirve para el emulador.

## Mapas y privacidad

El importador acepta JSON con `room_name` y `zones` (strings o objetos con
`label`). Sirve como fallback portable para mapas ARCore/RoomPlan. Todas las
posiciones se muestran como aproximadas y los frames no se persisten.
