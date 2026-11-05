package com.pulso.mobile.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.mobile.presentation.components.IconBadge
import com.pulso.mobile.presentation.components.IconCircleButton
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoLogo
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType

@Composable
fun OnboardingScreen(
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit,
    onAccept: () -> Unit
) {
    val c = Pulso.colors
    var agreed by rememberSaveable { mutableStateOf(false) }
    var showNotice by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            PulsoLogo(32.dp)
            Spacer(Modifier.size(10.dp))
            Text("Pulso", style = PulsoType.h2.copy(fontSize = 18.sp), color = c.onSurface, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("ES", "EN"),
                selectedIndex = language.ordinal,
                onSelect = { onLanguage(AppLanguage.entries[it]) },
                textStyle = PulsoType.chip
            )
            Spacer(Modifier.size(8.dp))
            IconCircleButton(
                if (c.isLight) PulsoIcons.Moon else PulsoIcons.Sun,
                contentDescription = tr("Cambiar tema", "Switch theme"),
                onClick = onToggleTheme
            )
        }

        HeroIllustration()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                tr("Tu pulso,\ntu calma.", "Your pulse,\nyour calm."),
                style = PulsoType.screenTitle.copy(fontSize = 32.sp, lineHeight = 35.sp),
                color = c.onSurface
            )
            Text(
                tr(
                    "Pulso lee el pulso y la HRV de tu reloj para notar la tensión a tiempo y guiarte de vuelta con la respiración.",
                    "Pulso reads your watch’s heart rate and HRV to notice tension early and guide you back with breathing."
                ),
                style = PulsoType.body, color = c.muted
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Feature(PulsoIcons.Watch, c.primary, c.primarySoft, tr("Detecta tensión desde tu muñeca", "Detects tension from your wrist"), tr("Comparada con tu propia línea base", "Compared against your own baseline"))
            Feature(PulsoIcons.Wind, c.hrv, c.hrvSoft, tr("Respira guiado por vibración", "Breathe guided by vibration"), tr("Sin tener que mirar la pantalla", "No need to look at the screen"))
            Feature(PulsoIcons.Shield, c.heart, c.heartSoft, tr("Tus datos se quedan en tus equipos", "Your data stays on your devices"), tr("Puedes retirar el consentimiento cuando quieras", "You can withdraw consent anytime"))
        }

        Spacer(Modifier.height(4.dp))

        AnimatedVisibility(showNotice) {
            PulsoCard(radius = 18.dp, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Aviso de bienestar y privacidad", "Wellness & privacy notice"), style = PulsoType.label.copy(fontSize = 14.sp), color = c.warn)
                listOf(
                    tr("Pulso es una herramienta de bienestar, no un dispositivo médico.", "Pulso is a wellness tool, not a medical device."),
                    tr("Mide periódicamente tu pulso para detectar patrones de tensión.", "It periodically reads your pulse to spot tension patterns."),
                    tr("Tus datos biométricos se quedan en tu reloj y tu celular.", "Your biometric data stays on your watch and phone."),
                    tr("No vendemos ni compartimos tus datos de salud.", "We never sell or share your health data."),
                    tr("El asistente usa IA para acompañarte; no reemplaza a un profesional.", "The assistant uses AI to support you; it’s not a substitute for a professional.")
                ).forEach { Text("• $it", style = PulsoType.bodySmall, color = c.muted) }
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.outline, RoundedCornerShape(18.dp))
                .clickable(role = Role.Checkbox) { agreed = !agreed }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (agreed) c.primary else c.surface)
                    .border(2.dp, if (agreed) c.primary else c.muted, RoundedCornerShape(6.dp))
                    .semantics { contentDescription = if (agreed) "checked" else "unchecked" },
                contentAlignment = Alignment.Center
            ) {
                if (agreed) Icon(PulsoIcons.Check, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
            }
            val consent = tr(
                "Leí el aviso de privacidad y entiendo que Pulso es una app de bienestar, no un dispositivo médico. ",
                "I’ve read the privacy notice and understand Pulso is a wellness app, not a medical device. "
            )
            val link = tr("Aviso de privacidad", "Privacy notice")
            Text(
                buildAnnotatedString {
                    append(consent)
                    withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.Bold)) { append(link) }
                },
                style = PulsoType.bodySmall,
                color = c.onSurface,
                modifier = Modifier.weight(1f).clickable { showNotice = !showNotice }
            )
        }

        PulsoButton(
            tr("Empezar", "Get started"),
            onAccept,
            Modifier.fillMaxWidth(),
            height = 56.dp,
            enabled = agreed,
            trailingIcon = PulsoIcons.ArrowRight,
            textStyle = PulsoType.button.copy(fontSize = 16.sp)
        )
    }
}

@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color, title: String, sub: String) {
    val c = Pulso.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon, tint, bg, iconSize = 22.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PulsoType.label, color = c.onSurface)
            Text(sub, style = PulsoType.bodySmall, color = c.muted)
        }
    }
}

@Composable
private fun HeroIllustration() {
    val c = Pulso.colors
    Canvas(Modifier.fillMaxWidth().height(214.dp)) {
        val s = size.height / 214f
        val cx = size.width / 2
        val cy = 107 * s
        drawCircle(c.primary.copy(alpha = 0.12f), 104 * s, Offset(cx, cy), style = Stroke(1.5f * s))
        drawCircle(c.primary.copy(alpha = 0.22f), 80 * s, Offset(cx, cy), style = Stroke(1.5f * s))
        drawCircle(c.primarySoft, 56 * s, Offset(cx, cy))
        drawCircle(c.primary.copy(alpha = 0.5f), 56 * s, Offset(cx, cy), style = Stroke(2 * s))
        val ox = cx - 171 * s
        val wave = Path().apply {
            moveTo(ox + 8 * s, 110 * s); lineTo(ox + 112 * s, 110 * s); lineTo(ox + 124 * s, 84 * s)
            lineTo(ox + 144 * s, 136 * s); lineTo(ox + 160 * s, 96 * s); lineTo(ox + 170 * s, 118 * s)
            lineTo(ox + 184 * s, 110 * s); lineTo(ox + 334 * s, 110 * s)
        }
        drawPath(wave, c.heart, style = Stroke(3.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(c.primary, 5 * s, Offset(cx, 3 * s + 2))
        drawCircle(c.hrv, 4 * s, Offset(ox + 275 * s, 107 * s))
        drawCircle(c.warn, 3 * s, Offset(ox + 91 * s, 51 * s))
        drawRoundRect(c.surface, Offset(ox + 250 * s, 150 * s), Size(54 * s, 54 * s), CornerRadius(27 * s))
        drawRoundRect(c.outline, Offset(ox + 250 * s, 150 * s), Size(54 * s, 54 * s), CornerRadius(27 * s), style = Stroke(1.5f * s))
        val mini = Path().apply {
            moveTo(ox + 264 * s, 177 * s); lineTo(ox + 272 * s, 177 * s); lineTo(ox + 275 * s, 170 * s)
            lineTo(ox + 280 * s, 184 * s); lineTo(ox + 283 * s, 177 * s); lineTo(ox + 290 * s, 177 * s)
        }
        drawPath(mini, c.primary, style = Stroke(2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
