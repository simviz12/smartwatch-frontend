package com.pulso.mobile.presentation.i18n

enum class AppLanguage {
    ES,
    EN
}

object PulsoStrings {
    fun title(lang: AppLanguage) = "PULSO"
    
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
    fun hrvRmssd(lang: AppLanguage) = if (lang == AppLanguage.ES) "VFC (RMSSD)" else "HRV (RMSSD)"
    fun statusLabel(lang: AppLanguage) = if (lang == AppLanguage.ES) "Estado" else "Status"
    fun noData(lang: AppLanguage) = if (lang == AppLanguage.ES) "Sin datos" else "No data"
    fun calmStatus(lang: AppLanguage) = if (lang == AppLanguage.ES) "En calma" else "Calm"
    fun activeStatus(lang: AppLanguage) = if (lang == AppLanguage.ES) "Activo" else "Active"
    fun hrTrendTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Frecuencia Cardíaca (Hoy)" else "Heart Rate (Today)"
    fun hrvTrendTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Variabilidad Cardíaca RMSSD (Hoy)" else "Heart Rate Variability (Today)"
    fun recentLogTitle(lang: AppLanguage) = if (lang == AppLanguage.ES) "Registro Reciente (Últimas 5 ventanas)" else "Recent Log (Last 5 windows)"

    // Assistant / Chat screen
    fun chatHeader(lang: AppLanguage) = if (lang == AppLanguage.ES) "Asistente Empático de Pulso" else "Pulso Empathic Assistant"
    fun chatSubHeader(lang: AppLanguage) = if (lang == AppLanguage.ES) "Acompañamiento emocional con voz e Inteligencia Artificial" else "Emotional support companion with voice and AI"
    fun micSpeak(lang: AppLanguage) = if (lang == AppLanguage.ES) "Hablar" else "Speak"
    fun micListening(lang: AppLanguage) = if (lang == AppLanguage.ES) "Escuchando..." else "Listening..."
    fun typeMessagePlaceholder(lang: AppLanguage) = if (lang == AppLanguage.ES) "Escribe o usa el micrófono..." else "Type or use the microphone..."
    fun send(lang: AppLanguage) = if (lang == AppLanguage.ES) "Enviar" else "Send"
    fun quickPrompt1(lang: AppLanguage) = if (lang == AppLanguage.ES) "Siento mucha presión en el pecho" else "I feel tension in my chest"
    fun quickPrompt2(lang: AppLanguage) = if (lang == AppLanguage.ES) "¿Podemos hacer un ejercicio de respiración?" else "Can we do a breathing exercise?"
    fun quickPrompt3(lang: AppLanguage) = if (lang == AppLanguage.ES) "Me siento abrumado con el trabajo" else "I feel overwhelmed with work"
    fun crisisNotice(lang: AppLanguage) = if (lang == AppLanguage.ES) 
        "Pulso es un asistente de bienestar emocional, no un servicio de emergencias médicas. Si estás en peligro, llama a la línea 106 o al 123." 
        else "Pulso is an emotional wellness companion, not an emergency service. In danger, call your local emergency line."
    fun initialBotGreeting(lang: AppLanguage) = if (lang == AppLanguage.ES)
        "Hola, soy tu compañero de Pulso. Noto que podrías estar experimentando tensión corporal. Estoy aquí para escucharte y ayudarte a encontrar calma a tu propio ritmo. ¿Qué está pasando por tu mente en este momento?"
        else "Hello, I am your Pulso companion. I noticed some physical tension. I'm here to listen and help you find calm at your own pace. What is on your mind right now?"
}
