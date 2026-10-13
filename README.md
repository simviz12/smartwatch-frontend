# Pulso - Wellness App

Pulso es una aplicación de bienestar compuesta por:
- **`:wear`**: Aplicación para Wear OS (reloj inteligente) que mide la frecuencia cardíaca y patrones de estrés, asistiendo con intervenciones táctiles y respiración guiada.
- **`:mobile`**: Aplicación complementaria para smartphone con historial, mapa de estrés, configuración y soporte.
- **`:core`**: Módulo Kotlin/JVM puro con la lógica de dominio, motores de estrés/HRV y contratos del Data Layer sin dependencias de Android.

---

## Requisitos Previos

- **JDK 21**
- **Android SDK** con API 35 instalada y herramientas de plataforma (`platform-tools` con `adb`)
- Reloj inteligente Wear OS (físico) y teléfono Android (físico)

---

## Ejecución de Pruebas

Para ejecutar las pruebas unitarias de `:core` y `:wear`:

```powershell
# Pruebas de :core
.\gradlew :core:test

# Pruebas de :wear
.\gradlew :wear:testDebugUnitTest

# Todas las pruebas unitarias
.\gradlew test
```

---

## Compilación de los APKs

```powershell
.\gradlew assembleDebug
```

Los APKs generados se ubican en:
- Reloj: `wear/build/outputs/apk/debug/wear-debug.apk`
- Celular: `mobile/build/outputs/apk/debug/mobile-debug.apk`

---

## Conexión e Instalación en el Reloj (Wear OS sobre Wi-Fi)

1. En el reloj inteligente:
   - Ve a **Ajustes > Opciones para desarrolladores**.
   - Activa **Depuración de ADB**.
   - Activa **Depuración inalámbrica (Wireless Debugging)**.
   - En la sección de depuración inalámbrica, consulta la dirección IP y el puerto (ejemplo: `192.168.1.150:5555` o puerto de emparejamiento).
   - Si requiere emparejamiento:
     ```powershell
     & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" pair <IP_RELOJ>:<PUERTO_PAIR> <CODIGO_PAIR>
     ```
2. Conectar por ADB:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" connect <IP_RELOJ>:<PUERTO>
   ```
3. Verificar conexión:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   ```
4. Instalar la app en el reloj:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <IP_RELOJ>:<PUERTO> install -r wear/build/outputs/apk/debug/wear-debug.apk
   ```

---

## Conexión e Instalación en el Celular

1. Conecta el teléfono por USB o Wi-Fi con depuración USB activada.
2. Instalar la app en el teléfono:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <SERIAL_CELULAR> install -r mobile/build/outputs/apk/debug/mobile-debug.apk
   ```

---

## Verificación de Logs en Tiempo Real

Para observar el envío de pings y el estado del Data Layer en el reloj:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <ID_DISPOSITIVO_RELOJ> logcat -v time -s PulsoWearPing
```
