# Diseño Háptico de Pulso (Haptics Guide)

Este documento describe la especificación formal del motor háptico de Pulso para Wear OS, implementado en `:core` y `:wear`.

---

## 1. Intervención Gentil (Alerta de Tensión / Estrés)

- **Nombre**: `HapticPattern.GENTLE_PULSE`
- **Propósito**: Llamar la atención del usuario de manera sutil e íntima, sin provocar sobresalto ni pánico.
- **Forma de onda (con control de amplitud)**:
  - Duraciones: `[0, 70, 140, 70]` ms (pausa inicial 0, pulso 70ms, silencio 140ms, pulso 70ms).
  - Amplitudes: `[0, 65, 0, 65]` (aproximadamente 25% de la potencia máxima de 255).
- **Fallback binario (sin control de amplitud)**:
  - Tiempos: `[0, 50, 120, 50]` ms (dos pulsos secos y breves para simular baja energía).
- **Sensación**: Un suave doble toque en la muñeca, semejante al roce de un dedo o un latido contenido.

---

## 2. Guía Háptica de Respiración (Breathing Timeline)

El ejercicio de respiración guiada permite su ejecución en modo **"Solo vibración"**, manteniendo la pantalla completamente apagada para favorecer la relajación ocular y la introspección.

### A. Componentes del Timeline

1. **Señal de Inicio (START_TAP)**:
   - Duración: 250 ms.
   - Perfil: Un pulso nítido y firme al 100% de la intensidad configurada, seguido de 500 ms de pausa para preparación.
   - Sensación: Una confirmación táctil inequívoca de que la sesión ha comenzado.

2. **Inhalación (INHALE)**:
   - Dinámica: Onda ascendente (**crescendo**).
   - Perfil de amplitudes: Crece progresivamente desde el 20% hasta el 100% de la amplitud máxima a lo largo de la duración (ej. 4.000 ms).
   - Sensación: Sensación continua de llenado o expansión en la muñeca.
   - **Fallback binario (sin control de amplitud)**:
     - Modulación por densidad de pulsos: Ráfaga rápida de pulsos densos (`60 ms activo, 50 ms inactivo`).
     - Sensación del fallback: Una vibración tipo "redoble" constante y rítmico que evoca subida o inhalación.

3. **Retención / Pausa (HOLD)**:
   - Dinámica: **Silencio absoluto** (`amplitud = 0`).
   - Tanto tras la inhalación (`HOLD_AFTER_INHALE`) como tras la exhalación (`HOLD_AFTER_EXHALE`), los actuadores permanecen en reposo completo.
   - Sensación: Calma e inmovilidad que marca la suspensión del aire.

4. **Exhalación (EXHALE)**:
   - Dinámica: Onda descendente (**decrescendo**).
   - Perfil de amplitudes: Desciende suavemente desde el 100% hasta el 15% de la amplitud a lo largo de la duración (ej. 6.000 ms u 8.000 ms).
   - Sensación: Descompresión gradual, relajación o desvanecimiento.
   - **Fallback binario (sin control de amplitud)**:
     - Modulación por densidad de pulsos: Pulsos espaciados y pausados (`80 ms activo, 160 ms inactivo`).
     - Sensación del fallback: Un ritmo lento y espaciado que evoca vaciado o exhalación.

5. **Señal de Finalización (END_DOUBLE_TAP)**:
   - Duración: 350 ms.
   - Perfil: Dos pulsos marcados con una breve interrupción (`100 ms on, 100 ms off, 150 ms on`).
   - Sensación: Un claro toque doble que avisa al usuario que el ciclo previsto ha concluido sin necesidad de mirar la pantalla.

---

## 3. Niveles de Intensidad Configurable

Se puede configurar la intensidad global desde el teléfono o el reloj (`HapticIntensity`):
- `LOW`: Amplitud máxima de 120 / 255 (~47%). Ideal para ambientes silenciosos o piel sensible.
- `MEDIUM`: Amplitud máxima de 180 / 255 (~70%). Recomendado por defecto.
- `HIGH`: Amplitud máxima de 240 / 255 (~94%). Mayor presencia para contextos cotidianos o movimiento.

---

## 4. Gestión de Energía y Seguridad

- Si el dispositivo entra en modo ahorro extremo de batería, la intensidad se reduce automáticamente a `LOW`.
- Ante interrupciones (llamadas, alarmas o toque deliberado para detener), el servicio cancela inmediatamente la vibración activa invocando `vibrator.cancel()`.
