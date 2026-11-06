# Pulso — Bienestar en tu muñeca

Pulso detecta la tensión a partir del pulso y la HRV de tu reloj Wear OS, te avisa a tiempo y te guía de vuelta a la calma con respiración por vibración, un asistente por voz y un historial claro en el celular.

## ⬇️ Descargar

<p align="center">
  <a href="https://github.com/simviz12/smartwatch-frontend/releases/latest/download/pulso-celular.apk">
    <img alt="Descargar app del celular" src="https://img.shields.io/badge/Descargar-App%20del%20celular-0B7D69?style=for-the-badge&logo=android&logoColor=white">
  </a>
  <a href="https://github.com/simviz12/smartwatch-frontend/releases/latest/download/pulso-reloj.apk">
    <img alt="Descargar app del reloj" src="https://img.shields.io/badge/Descargar-App%20del%20reloj-121A22?style=for-the-badge&logo=wearos&logoColor=white">
  </a>
  <a href="https://github.com/simviz12/smartwatch-frontend/releases/latest/download/pulso-esfera-reloj.apk">
    <img alt="Descargar esfera del reloj" src="https://img.shields.io/badge/Descargar-Esfera%20del%20reloj-4A55C9?style=for-the-badge&logo=wearos&logoColor=white">
  </a>
</p>

<p align="center">
  <a href="https://github.com/simviz12/smartwatch-frontend/releases/latest"><img alt="Última versión" src="https://img.shields.io/github/v/release/simviz12/smartwatch-frontend?label=versi%C3%B3n&style=flat-square"></a>
  <a href="https://github.com/simviz12/smartwatch-frontend/actions/workflows/release-apk.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/simviz12/smartwatch-frontend/release-apk.yml?branch=main&label=build&style=flat-square"></a>
  <img alt="Android 8+" src="https://img.shields.io/badge/Android-8%2B-3DD6B5?style=flat-square&logo=android&logoColor=white">
  <img alt="Wear OS 3+" src="https://img.shields.io/badge/Wear%20OS-3%2B-9AA6FF?style=flat-square&logo=wearos&logoColor=white">
</p>

| Archivo | Para qué | Requisitos |
|---|---|---|
| `pulso-celular.apk` | App del teléfono: inicio de sesión, Hoy, Historial, Respirar, Asistente y Ajustes | Android 8 o superior |
| `pulso-reloj.apk` | App del reloj, Tile y complicaciones (pulso, HRV, estado) | Wear OS 3 o superior |
| `pulso-esfera-reloj.apk` | Esfera de Pulso (Watch Face Format). Se instala junto con la app del reloj | Wear OS 4 o superior |

> **¿Cómo instalar?** Descarga el APK en el teléfono, ábrelo y permite "instalar apps de origen desconocido". En el reloj se instala con `adb` (ver [Conexión e instalación en el reloj](#conexión-e-instalación-en-el-reloj-wear-os-sobre-wi-fi)).

## 🔐 Cuenta de prueba

La app pide iniciar sesión contra el backend de Pulso. Se puede **crear una cuenta** desde la app o entrar con la cuenta demo:

- **Correo:** `profesor@pulso.app`
- **Contraseña:** la que se configuró como `DEMO_PASSWORD` al desplegar el backend.

> La primera entrada del día puede tardar hasta 1 minuto: el servidor gratuito estaba dormido y la app lo indica.

## 🚀 Despliegue completo

Backend en Render + APK conectados en GitHub Releases, paso a paso: **[docs/DESPLIEGUE.md](docs/DESPLIEGUE.md)**.

Resumen:
1. **Render** › New › Blueprint › repositorio [`smartwatch-backend`](https://github.com/simviz12/smartwatch-backend) › define `DEMO_PASSWORD` › Apply.
2. **GitHub** › este repositorio › Settings › Secrets › Actions › `PULSO_API_URL` = URL de Render.
3. **Releases** › nueva versión (ej. `v1.2.0`) → GitHub compila y adjunta los tres APK.

---

## Arquitectura

Pulso está compuesto por:
- **`:wear`**: Aplicación para Wear OS (reloj inteligente) que mide la frecuencia cardíaca y patrones de estrés, asistiendo con intervenciones táctiles y respiración guiada.
- **`:mobile`**: Aplicación complementaria para smartphone con historial, mapa de estrés, configuración y soporte.
- **`:core`**: Módulo Kotlin/JVM puro con la lógica de dominio, motores de estrés/HRV y contratos del Data Layer sin dependencias de Android.
- **`:watchface`**: Esfera en Watch Face Format (XML, sin código).
- **Backend:** [`smartwatch-backend`](https://github.com/simviz12/smartwatch-backend) (NestJS + PostgreSQL) para cuentas e inicio de sesión.

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
- Esfera del reloj (Watch Face Format): `watchface/build/outputs/apk/debug/watchface-debug.apk`

### Esfera, Tile y complicaciones

Wear OS 5+ solo acepta esferas en **Watch Face Format** (XML, sin código), por eso la esfera vive en el módulo `:watchface`.
Sus datos (pulso, HRV y estado) los publica la app del reloj como complicaciones, así que deben instalarse ambos APK en el reloj.

```powershell
adb -s <RELOJ> install -r watchface/build/outputs/apk/debug/watchface-debug.apk
# Poner la esfera de Pulso
adb -s <RELOJ> shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation set-watchface --es watchFaceId com.pulso.watchface
# Agregar y mostrar el Tile de Pulso
adb -s <RELOJ> shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation add-tile --ecn component com.pulso/com.pulso.wear.tile.PulsoTileService
```

El XML de la esfera se genera con `node watchface/generate-watchface.js watchface/src/main/res/raw/watchface.xml`.

### Datos de ejemplo

- **Celular:** Historial › Mapa › *Datos de ejemplo*. Quedan marcados (`source = DEMO`, ids `demo-…`) y *Quitar ejemplo* borra solo esos; los datos reales y el consentimiento se conservan.
- **Reloj:** Sensores › *Modo demo* muestra valores de ejemplo en la esfera, el Tile y las complicaciones. Cualquier lectura real reciente tiene prioridad y nada de ejemplo se sincroniza con el celular.

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
