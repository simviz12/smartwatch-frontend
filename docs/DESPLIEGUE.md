# Guía completa de despliegue de Pulso

Esta guía deja Pulso funcionando de punta a punta para que cualquier persona (por ejemplo, un profesor) pueda **descargar la app, crear una cuenta o entrar con la cuenta demo** y usarla.

Pulso tiene tres piezas:

| Pieza | Repositorio | Dónde vive |
|---|---|---|
| Backend (API de inicio de sesión) | [`smartwatch-backend`](https://github.com/simviz12/smartwatch-backend) | **Render** (gratis) |
| App del celular | [`smartwatch-frontend`](https://github.com/simviz12/smartwatch-frontend) › `mobile` | APK en **GitHub Releases** |
| App, Tile y esfera del reloj | [`smartwatch-frontend`](https://github.com/simviz12/smartwatch-frontend) › `wear` y `watchface` | APK en **GitHub Releases** |

Tiempo total: **unos 20 minutos**. No necesitas instalar nada en tu computador: Render y GitHub compilan todo.

---

## Paso 1 · Desplegar el backend en Render (≈ 10 min)

1. Entra a **https://render.com** y pulsa **Get Started** → **GitHub**. Autoriza a Render a ver tus repositorios.
2. En el panel de Render pulsa **New +** (arriba a la derecha) → **Blueprint**.
3. Conecta el repositorio **`simviz12/smartwatch-backend`** y pulsa **Connect**.
   Render lee el archivo [`render.yaml`](https://github.com/simviz12/smartwatch-backend/blob/main/render.yaml) y te muestra lo que va a crear:
   - `pulso-db` → base de datos PostgreSQL (plan gratis).
   - `pulso-api` → el servicio web del backend (plan gratis).
4. Dale un nombre al Blueprint (por ejemplo `pulso`).
5. Render te pide el valor de **`DEMO_PASSWORD`**. Escribe la contraseña que usará tu profesor (mínimo **8 caracteres**, por ejemplo `Pulso2026!`). **Anótala.**
6. Pulsa **Apply**. Render:
   - crea la base de datos,
   - instala dependencias y compila (`npm ci`, `prisma generate`, `nest build`),
   - aplica las migraciones (`prisma migrate deploy`),
   - arranca la API y crea la cuenta demo `profesor@pulso.app`.
7. Espera a que `pulso-api` diga **Live** (5–8 min la primera vez).
8. Abre el servicio `pulso-api` y copia su URL, arriba a la izquierda. Se ve así:
   `https://pulso-api-xxxx.onrender.com`
9. **Comprueba que funciona** abriendo en el navegador:
   - `https://pulso-api-xxxx.onrender.com/health` → debe mostrar `"status":"ok"` y `"database":"up"`.
   - `https://pulso-api-xxxx.onrender.com/docs` → documentación interactiva (Swagger). Ahí puedes probar `POST /v1/auth/login` con `profesor@pulso.app` y tu contraseña.

> Si `/health` dice `"database":"down"`, espera un minuto (la base de datos puede seguir arrancando) y recarga.

---

## Paso 2 · Conectar la app a tu backend (≈ 3 min)

La app necesita saber la URL de tu backend. Se la damos a GitHub como *secret* para que la incluya al compilar.

1. Ve a **https://github.com/simviz12/smartwatch-frontend** → **Settings** → **Secrets and variables** → **Actions**.
2. Pulsa **New repository secret**:
   - **Name:** `PULSO_API_URL`
   - **Secret:** la URL del paso 1.8, **sin** `/` al final (ej. `https://pulso-api-xxxx.onrender.com`)
   - Pulsa **Add secret**.
3. *(Opcional)* Para que el asistente responda con IA, crea otro secret:
   - **Name:** `GEMINI_API_KEY`
   - **Secret:** tu clave de [Google AI Studio](https://aistudio.google.com/apikey).
   - Ten en cuenta que la clave queda dentro del APK.

---

## Paso 3 · Publicar los APK con tu backend (≈ 7 min)

1. Ve a **https://github.com/simviz12/smartwatch-frontend/releases** → **Draft a new release**.
2. En **Choose a tag** escribe una versión nueva, por ejemplo `v1.2.0`, y pulsa **Create new tag**.
3. Título: `Pulso v1.2.0`. Pulsa **Publish release**.
4. Ve a la pestaña **Actions**: el workflow **Build APKs & Release** se ejecuta solo. Cuando termine (≈ 6 min, check verde), la release tendrá:
   - `pulso-celular.apk`
   - `pulso-reloj.apk`
   - `pulso-esfera-reloj.apk`
5. Los botones de descarga del README apuntan siempre a la **última release**, así que se actualizan solos.

> El workflow también corre en cada push a `main` y deja los APK como *artefacto* en la pestaña Actions, por si quieres probar algo sin publicar una versión.

---

## Paso 4 · Instalar y entrar (lo que hace el profesor)

### En el celular (Android 8 o superior)
1. Descarga **`pulso-celular.apk`** desde el README o la release.
2. Ábrelo. Android pedirá permitir **instalar apps de origen desconocido** para el navegador o el gestor de archivos: acéptalo.
3. Abre **Pulso**. La app arranca en tema claro, en **Iniciar sesión**.
4. Entra con:
   - **Correo:** `profesor@pulso.app`
   - **Contraseña:** la del paso 1.5

   O pulsa **Crear cuenta** y regístrate con su propio correo.
5. La primera vez puede tardar **hasta 1 minuto** (el servidor gratuito estaba dormido); la app muestra un aviso mientras tanto.
6. Acepta el aviso de privacidad. Para ver la app con datos sin tener reloj: **Historial › Mapa › Datos de ejemplo**.

### En el reloj (opcional, Wear OS 3 o superior)
1. Activa en el reloj **Opciones de desarrollador › Depuración ADB y Depuración inalámbrica** (ver [README › Conexión e instalación en el reloj](../README.md#conexión-e-instalación-en-el-reloj-wear-os-sobre-wi-fi)).
2. Instala desde un computador:
   ```powershell
   adb connect <IP_RELOJ>:<PUERTO>
   adb -s <IP_RELOJ>:<PUERTO> install -r pulso-reloj.apk
   adb -s <IP_RELOJ>:<PUERTO> install -r pulso-esfera-reloj.apk
   ```
3. En el reloj: mantén pulsada la carátula para elegir la **esfera Pulso**, y desliza a la izquierda → **+** para añadir el **Tile Pulso**.
4. Para que reloj y celular hablen (sincronización, asistente por voz), deben estar **emparejados** en la app Wear OS / Pixel Watch.

---

## Comprobación rápida

| Qué probar | Resultado esperado |
|---|---|
| `https://<tu-url>/health` | `"status":"ok"`, `"database":"up"` |
| Entrar con la cuenta demo | Lleva a la bienvenida / Hoy |
| Contraseña incorrecta | "Correo o contraseña incorrectos." |
| Crear cuenta con un correo ya usado | "Ya existe una cuenta con ese correo." |
| Ajustes › Cuenta › Salir | Vuelve a la pantalla de inicio de sesión |

---

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| La app dice **"Servidor no configurado"** | El APK se compiló sin `PULSO_API_URL`. Haz el paso 2 y vuelve a publicar (paso 3). |
| La app dice **"Sin conexión con el servidor"** | Revisa internet y que `/health` responda. Comprueba que el secret no tenga `/` al final ni espacios. |
| El primer inicio tarda ~1 minuto | Normal en el plan gratuito: Render duerme el servicio tras 15 min sin uso. |
| Render: el deploy falla en `prisma migrate deploy` | La base de datos aún no estaba lista. En `pulso-api` pulsa **Manual Deploy › Deploy latest commit**. |
| Olvidé la contraseña demo | La cuenta demo solo se crea la primera vez; cambiar `DEMO_PASSWORD` después no la modifica. Lo más simple: crea una cuenta nueva desde la app con **Crear cuenta**. |
| Android bloquea la instalación | Activa "Instalar apps desconocidas" para el navegador o el gestor de archivos que usaste. |

---

## Límites del plan gratuito

- **Render web service:** se duerme tras 15 min sin peticiones; despierta con la siguiente (~1 min).
- **Render PostgreSQL gratis:** caduca a los **30 días**. Antes de esa fecha, crea una nueva base de datos (o pasa a un plan de pago) y vuelve a desplegar.
- **APK de depuración:** firmados con la clave de debug, aptos para pruebas y entregas académicas, no para Google Play.
