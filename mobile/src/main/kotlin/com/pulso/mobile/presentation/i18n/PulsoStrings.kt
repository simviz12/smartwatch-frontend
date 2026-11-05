package com.pulso.mobile.presentation.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppLanguage { ES, EN }

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ES }

/** Inline bilingual string: every screen passes both copies so ES/EN stay in sync. */
@Composable
@ReadOnlyComposable
fun tr(es: String, en: String): String = if (LocalAppLanguage.current == AppLanguage.EN) en else es

@Composable
@ReadOnlyComposable
fun isEnglish(): Boolean = LocalAppLanguage.current == AppLanguage.EN

fun AppLanguage.locale(): Locale = if (this == AppLanguage.EN) Locale.ENGLISH else Locale("es", "CO")

/** "lpm" in Spanish, "bpm" in English. */
@Composable
@ReadOnlyComposable
fun bpmUnit(): String = tr("lpm", "bpm")

fun formatTime(time: Long, language: AppLanguage): String =
    SimpleDateFormat("HH:mm", language.locale()).format(Date(time))

/** Header date, e.g. "Miércoles 4 de noviembre" / "Wednesday, Nov 4". */
fun formatLongDate(time: Long, language: AppLanguage): String {
    val pattern = if (language == AppLanguage.EN) "EEEE, MMM d" else "EEEE d 'de' MMMM"
    return SimpleDateFormat(pattern, language.locale()).format(Date(time))
        .replaceFirstChar { it.titlecase(language.locale()) }
}

fun formatShortDate(time: Long, language: AppLanguage): String {
    val pattern = if (language == AppLanguage.EN) "MMM d" else "d MMM"
    return SimpleDateFormat(pattern, language.locale()).format(Date(time)).trimEnd('.')
}

/**
 * Status messages produced by the ViewModel/sync layer are written in Spanish.
 * This shows them in the UI language and hides raw exception text from the user.
 */
@Composable
@ReadOnlyComposable
fun localizeStatus(message: String?): String? {
    if (message.isNullOrBlank()) return null
    val en = isEnglish()
    fun t(es: String, enText: String) = if (en) enText else es
    Regex("Ping exitoso: (\\d+)ms").find(message)?.let { return t("Conexión con el reloj OK · ${it.groupValues[1]} ms", "Watch connection OK · ${it.groupValues[1]} ms") }
    Regex("Datos de ejemplo listos: (\\d+) mediciones, (\\d+) eventos y (\\d+) sesiones").find(message)?.let {
        val (w, e, s) = it.destructured
        return t("Datos de ejemplo listos: $w mediciones, $e eventos y $s sesiones.", "Sample data ready: $w readings, $e events and $s sessions.")
    }
    Regex("Datos de ejemplo quitados \\((\\d+) mediciones, (\\d+) eventos, (\\d+) sesiones\\)").find(message)?.let {
        val (w, e, s) = it.destructured
        return t("Datos de ejemplo quitados ($w mediciones, $e eventos, $s sesiones). Tus datos reales se conservan.", "Sample data removed ($w readings, $e events, $s sessions). Your real data is kept.")
    }
    Regex("Generando (\\d+) días").find(message)?.let { return t("Generando ${it.groupValues[1]} días de datos de ejemplo…", "Generating ${it.groupValues[1]} days of sample data…") }
    Regex("No hay mediciones registradas en el período seleccionado \\((\\d+) días\\)").find(message)?.let {
        return t("No hay mediciones en los últimos ${it.groupValues[1]} días.", "No readings in the last ${it.groupValues[1]} days.")
    }
    return when {
        message.startsWith("Configuración sincronizada") -> t("Guardado y enviado al reloj.", "Saved and sent to the watch.")
        message.startsWith("Guardado localmente") -> t("Guardado en el celular. Se enviará al reloj cuando se conecte.", "Saved on this phone. It will reach the watch when it connects.")
        message.startsWith("Sesión iniciada en el reloj") -> t("Sesión iniciada en el reloj.", "Session started on the watch.")
        message.startsWith("Comando de reinicio enviado") -> t("Reinicio enviado al reloj.", "Reset sent to the watch.")
        message.startsWith("No se pudo comunicar") || message.startsWith("No se pudo conectar") ->
            t("No se pudo conectar con el reloj. Revisa que esté emparejado y cerca.", "Couldn’t reach the watch. Make sure it’s paired and nearby.")
        message.startsWith("Todos tus datos locales") -> t("Tus datos locales se borraron.", "Your local data was deleted.")
        message.contains("Wearable.API is not available") || message.contains("API_UNAVAILABLE") ->
            t("Este celular no tiene los servicios de Wear OS (instala la app del reloj).", "This phone has no Wear OS services (install the watch companion app).")
        message.startsWith("Fallo") || message.startsWith("Error") || message.contains("Exception") ->
            t("Falló la comunicación con el reloj.", "Communication with the watch failed.")
        message.startsWith("No hay reloj conectado") || message.contains("reloj no conectado") -> t("No hay reloj conectado.", "No watch connected.")
        else -> message
    }
}

fun minutesAgoLabel(minutes: Long, language: AppLanguage): String = when {
    language == AppLanguage.EN && minutes < 1 -> "Just now"
    language == AppLanguage.EN && minutes < 60 -> "$minutes min ago"
    language == AppLanguage.EN -> "${minutes / 60} h ago"
    minutes < 1 -> "Justo ahora"
    minutes < 60 -> "Hace $minutes min"
    else -> "Hace ${minutes / 60} h"
}
