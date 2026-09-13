# ONE Android

La app Android usa Kotlin y Jetpack Compose, con CameraX para capturas
muestreadas y LiveKit para publicación en tiempo real. El backend procesa los
frames en memoria; la app no guarda vídeo localmente.

## Desarrollo

1. Arranca el backend (`one-backend`) en `http://127.0.0.1:8000`.
2. Abre este proyecto en Android Studio y ejecuta el emulador Pixel.
3. El emulador usa `http://10.0.2.2:8000/api/v1` por defecto.
4. Para apuntar a otra instancia: `gradlew.bat assembleDebug -PoneApiBaseUrl=https://dev.example/api/v1`.

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

## Mapas y privacidad

El importador acepta JSON con `room_name` y `zones` (strings o objetos con
`label`). Sirve como fallback portable para mapas ARCore/RoomPlan. Todas las
posiciones se muestran como aproximadas y los frames no se persisten.
