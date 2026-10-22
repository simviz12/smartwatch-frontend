package com.pulso.mobile.presentation.i18n

enum class AppLanguage {
    ES,
    EN
}

object PulsoStrings {
    fun title(lang: AppLanguage) = "PULSO"
    
    // Top bar theme labels
    fun themeLight(lang: AppLanguage) = if (lang == AppLanguage.ES) "Claro" else "Light"
    fun themeDark(lang: AppLanguage) = if (lang == AppLanguage.ES) "Oscuro" else "Dark"

    // Bottom Bar tabs
    fun tabToday(lang: AppLanguage) = if (lang == AppLanguage.ES) "Hoy" else "Today"
    fun tabEvents(lang: AppLanguage) = if (lang == AppLanguage.ES) "Eventos" else "Events"
    fun tabBreathe(lang: AppLanguage) = if (lang == AppLanguage.ES) "Respirar" else "Breathe"
    fun tabMap(lang: AppLanguage) = if (lang == AppLanguage.ES) "Mapa" else "Map"
    fun tabChat(lang: AppLanguage) = if (lang == AppLanguage.ES) "Asistente" else "Assistant"
    fun tabDiagnostics(lang: AppLanguage) = if (lang == AppLanguage.ES) "Diagnóstico" else "Diagnostics"
    fun tabSettings(lang: AppLanguage) = if (lang == AppLanguage.ES) "Ajustes" else "Settings"

    // Today screen
    fun todayTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Resumen de Hoy" else "Today's Summary"
    fun yourBaseline(lang: AppLanguage) = if (lang == AppLanguage.ES) "Tu línea base ›" else "Your baseline ›"
    fun latestPulse(lang: AppLanguage) = if (lang == AppLanguage.ES) "Último Pulso" else "Latest Pulse"
    fun hrvRmssd(lang: AppLanguage) = if (lang == AppLanguage.ES) "Variabilidad (HRV)" else "Variability (HRV)"
    fun statusLabel(lang: AppLanguage) = if (lang == AppLanguage.ES) "Estado" else "Status"
    fun noData(lang: AppLanguage) = if (lang == AppLanguage.ES) "Sin datos" else "No data"
    fun calmStatus(lang: AppLanguage) = if (lang == AppLanguage.ES) "En calma" else "Calm"
    fun activeStatus(lang: AppLanguage) = if (lang == AppLanguage.ES) "Activo" else "Active"
    fun measurementsToday(lang: AppLanguage) = if (lang == AppLanguage.ES) "Mediciones hoy" else "Measurements today"
    fun windowsCount(lang: AppLanguage, count: Int) = if (lang == AppLanguage.ES) "$count ventanas" else "$count windows"
    fun minutesAgo(lang: AppLanguage, min: Long) = if (lang == AppLanguage.ES) "Hace $min min" else "$min min ago"
    fun hrTrendTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Frecuencia Cardíaca (BPM)" else "Heart Rate (BPM)"
    fun hrvTrendTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Variabilidad RMSSD (ms)" else "RMSSD Variability (ms)"
    fun rangeLabel(lang: AppLanguage, min: Int, max: Int, unit: String) = 
        if (lang == AppLanguage.ES) "Franja: $min-$max $unit" else "Range: $min-$max $unit"
    fun emptyHrMessage(lang: AppLanguage) = if (lang == AppLanguage.ES) "Sin datos suficientes para graficar hoy" else "Not enough data to graph today"
    fun emptyHrvMessage(lang: AppLanguage) = if (lang == AppLanguage.ES) "HRV no disponible en este reloj o sin muestras IBI" else "HRV not available or no IBI samples"

    // Assistant / Chat screen
    fun chatHeader(lang: AppLanguage) = if (lang == AppLanguage.ES) "Asistente Empático de Pulso" else "Pulso Empathic Assistant"
    fun chatSubHeader(lang: AppLanguage) = if (lang == AppLanguage.ES) "Acompañamiento emocional con voz e IA" else "Emotional support companion with voice & AI"
    fun micSpeak(lang: AppLanguage) = if (lang == AppLanguage.ES) "Hablar" else "Speak"
    fun micListening(lang: AppLanguage) = if (lang == AppLanguage.ES) "Escuchando..." else "Listening..."
    fun typeMessagePlaceholder(lang: AppLanguage) = if (lang == AppLanguage.ES) "Escribe o usa el micrófono..." else "Type or use the microphone..."
    fun send(lang: AppLanguage) = if (lang == AppLanguage.ES) "Enviar" else "Send"
    fun pauseAudio(lang: AppLanguage) = if (lang == AppLanguage.ES) "Pausar audio" else "Pause audio"
    fun quickPrompt1(lang: AppLanguage) = if (lang == AppLanguage.ES) "Siento mucha presión en el pecho" else "I feel tension in my chest"
    fun quickPrompt2(lang: AppLanguage) = if (lang == AppLanguage.ES) "¿Podemos hacer un ejercicio de respiración?" else "Can we do a breathing exercise?"
    fun quickPrompt3(lang: AppLanguage) = if (lang == AppLanguage.ES) "Me siento abrumado con el trabajo" else "I feel overwhelmed with work"
    fun crisisNotice(lang: AppLanguage) = if (lang == AppLanguage.ES) 
        "Pulso es un asistente de bienestar emocional, no un servicio de emergencias médicas. Si estás en peligro, llama a la línea 106 o al 123." 
        else "Pulso is an emotional wellness companion, not an emergency service. In danger, call your local emergency line."
    fun initialBotGreeting(lang: AppLanguage) = if (lang == AppLanguage.ES)
        "Hola, soy tu compañero de Pulso. Noto que podrías estar experimentando tensión corporal. Estoy aquí para escucharte y ayudarte a encontrar calma a tu propio ritmo. ¿Qué está pasando por tu mente en este momento?"
        else "Hello, I am your Pulso companion. I noticed some physical tension. I'm here to listen and help you find calm at your own pace. What is on your mind right now?"

    // Breathing Screen
    fun breatheTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Respiración Guiada" else "Guided Breathing"
    fun weeklySummaryTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Resumen Semanal" else "Weekly Summary"
    fun sessionsCount(lang: AppLanguage, count: Int) = if (lang == AppLanguage.ES) "$count sesiones" else "$count sessions"
    fun last7Days(lang: AppLanguage) = if (lang == AppLanguage.ES) "Últimos 7 días" else "Last 7 days"
    fun totalTime(lang: AppLanguage) = if (lang == AppLanguage.ES) "Tiempo total" else "Total time"
    fun feltBetter(lang: AppLanguage) = if (lang == AppLanguage.ES) "Se sintieron mejor" else "Felt better"
    fun testOnWatch(lang: AppLanguage) = if (lang == AppLanguage.ES) "Probar ejercicio en el reloj" else "Test exercise on watch"
    fun testingOnWatch(lang: AppLanguage) = if (lang == AppLanguage.ES) "Iniciando en reloj..." else "Starting on watch..."

    // Events Screen
    fun eventsTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Historial de Eventos" else "Event History"
    fun noEventsRegistered(lang: AppLanguage) = if (lang == AppLanguage.ES) "No hay eventos registrados aún" else "No events recorded yet"
    fun backToHome(lang: AppLanguage) = if (lang == AppLanguage.ES) "Volver al inicio" else "Back to home"

    // Diagnostics Screen
    fun diagnosticsTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Diagnóstico de Pulso" else "Pulso Diagnostics"
    fun consentCardTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Consentimiento" else "Consent"
    fun watchConnectionTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Conexión con el Reloj" else "Watch Connection"
    fun storageSyncTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Almacenamiento y Sincronización" else "Storage & Sync"
    fun detectionEventsTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Detección y Eventos" else "Detection & Events"
    fun hapticCardTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Respiración y Háptica" else "Breathing & Haptics"
    fun testWatchConnection(lang: AppLanguage) = if (lang == AppLanguage.ES) "Probar conexión con el reloj" else "Test watch connection"

    // Settings Screen
    fun settingsTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Ajustes y Parámetros" else "Settings & Parameters"
    fun sensitivityTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Sensibilidad de Detección" else "Detection Sensitivity"
    fun sensitivityDesc(lang: AppLanguage) = if (lang == AppLanguage.ES) 
        "Define qué tan marcado debe ser el cambio en tu pulso y variabilidad antes de identificar una posible tensión."
        else "Defines how significant heart rate and HRV changes must be before identifying possible stress."
    fun quietHoursTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Horas de Silencio (Sin Avisos)" else "Quiet Hours (No Alerts)"
    fun saveConfig(lang: AppLanguage) = if (lang == AppLanguage.ES) "Guardar Configuración" else "Save Settings"
    fun savingConfig(lang: AppLanguage) = if (lang == AppLanguage.ES) "Guardando..." else "Saving..."
    fun revokeConsent(lang: AppLanguage) = if (lang == AppLanguage.ES) "Retirar consentimiento de medición" else "Revoke measurement consent"
    fun revokeConsentWarning(lang: AppLanguage) = if (lang == AppLanguage.ES) 
        "Al retirar tu consentimiento, el reloj inteligente detendrá inmediatamente cualquier monitoreo y recolección de pulso."
        else "Revoking consent will immediately stop any pulse monitoring and data collection on the smartwatch."
    fun baselineCardTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Línea Base Personal" else "Personal Baseline"
    fun baselineCardDesc(lang: AppLanguage) = if (lang == AppLanguage.ES) 
        "Consulta el aprendizaje de tus rangos normales de pulso y variabilidad para cada horario del día."
        else "Check the learned normal ranges for heart rate and variability throughout the day."
    fun viewMyBaseline(lang: AppLanguage) = if (lang == AppLanguage.ES) "Ver mi línea base" else "View my baseline"
    fun breathingHapticTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Respiración Guiada Háptica" else "Haptic Guided Breathing"
    fun breathingHapticDesc(lang: AppLanguage) = if (lang == AppLanguage.ES)
        "Configura patrones de respiración (Calma 4-6, Cuadrada, etc.), duración, intensidad y prueba sesiones en el reloj."
        else "Configure breathing patterns (Calm 4-6, Box, etc.), duration, intensity, and test sessions on the watch."
    fun configureBreathing(lang: AppLanguage) = if (lang == AppLanguage.ES) "Configurar y ver historial de respiración ›" else "Configure and view breathing history ›"

    // Baseline Screen
    fun baselineTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Línea Base Personal" else "Personal Baseline"
    fun resetBaseline(lang: AppLanguage) = if (lang == AppLanguage.ES) "Reiniciar aprendizaje" else "Reset learning"

    // Stress Map
    fun stressMapTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Mapa de Tensión" else "Stress Map"
}
