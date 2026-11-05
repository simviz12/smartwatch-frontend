package com.pulso.mobile.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.intervention.AdaptiveSuggestion
import com.pulso.core.model.QuietHours
import com.pulso.core.stress.StressConfig
import com.pulso.mobile.domain.intervention.QualityMetrics
import com.pulso.mobile.domain.stress.SensitivityPreset
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.FeedbackText
import com.pulso.mobile.presentation.components.ListRow
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoButtonStyle
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoConfirmSheet
import com.pulso.mobile.presentation.components.ScreenHeader
import com.pulso.mobile.presentation.components.SectionLabel
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.components.Stepper
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType
import com.pulso.mobile.presentation.theme.ThemeMode
import java.util.Locale
import kotlin.math.roundToInt

private val QUIET_PRESETS = listOf(
    QuietHours(22, 0, 7, 0), QuietHours(23, 0, 8, 0), QuietHours(21, 0, 6, 0), QuietHours(0, 0, 7, 0)
)

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    language: AppLanguage,
    config: StressConfig,
    preset: SensitivityPreset,
    quietHours: QuietHours,
    qualityMetrics: QualityMetrics,
    adaptiveSuggestion: AdaptiveSuggestion?,
    isSaving: Boolean,
    saveFeedback: String?,
    onThemeMode: (ThemeMode) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onPreset: (SensitivityPreset) -> Unit,
    onUpdateThresholds: (hrZ: Double, hrvZ: Double, hrOnlyZ: Double, consecA: Int, consecB: Int, cooldown: Int, dailyCap: Int) -> Unit,
    onQuietHours: (QuietHours) -> Unit,
    onResetDefaults: () -> Unit,
    onSave: () -> Unit,
    onCancelSnooze: () -> Unit,
    onApplySuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit,
    onOpenBaseline: () -> Unit,
    onOpenLab: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onRevokeConsent: () -> Unit,
    onWipeData: () -> Unit,
    accountName: String = "",
    accountEmail: String = "",
    onSignOut: () -> Unit = {}
) {
    val c = Pulso.colors
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val fmt: (Int, Int) -> String = { h, m -> String.format(Locale.US, "%02d:%02d", h, m) }
    val update: (StressConfig) -> Unit = {
        onUpdateThresholds(it.hrZThreshold, it.hrvZThreshold, it.hrOnlyZThreshold, it.consecutiveWindowsRuleA, it.consecutiveWindowsRuleB, it.cooldownMinutes, it.dailyCap)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenHeader(title = tr("Ajustes", "Settings"))

        if (accountEmail.isNotBlank()) {
            SectionLabel(tr("Cuenta", "Account"), Modifier.padding(top = 4.dp))
            PulsoCard(modifier = Modifier.reveal(0), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.primarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(accountName.ifBlank { accountEmail }.take(1).uppercase(), style = PulsoType.title, color = c.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(accountName.ifBlank { tr("Tu cuenta", "Your account") }, style = PulsoType.label, color = c.onSurface)
                        Text(accountEmail, style = PulsoType.caption, color = c.muted)
                    }
                    PulsoButton(
                        tr("Salir", "Sign out"), { confirmSignOut = true },
                        style = PulsoButtonStyle.Ghost, height = 40.dp,
                        textStyle = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }

        SectionLabel(tr("Apariencia", "Appearance"), Modifier.padding(top = 4.dp))
        PulsoCard(modifier = Modifier.reveal(1), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(PulsoIcons.Moon, null, tint = c.muted, modifier = Modifier.size(20.dp))
                Text(tr("Tema", "Theme"), style = PulsoType.label, color = c.onSurface, modifier = Modifier.weight(1f))
                SegmentedControl(
                    options = listOf(tr("Sistema", "System"), tr("Claro", "Light"), tr("Oscuro", "Dark")),
                    selectedIndex = themeMode.ordinal,
                    onSelect = { onThemeMode(ThemeMode.entries[it]) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(PulsoIcons.Globe, null, tint = c.muted, modifier = Modifier.size(20.dp))
                Text(tr("Idioma", "Language"), style = PulsoType.label, color = c.onSurface, modifier = Modifier.weight(1f))
                SegmentedControl(
                    options = listOf("Español", "English"),
                    selectedIndex = language.ordinal,
                    onSelect = { onLanguage(AppLanguage.entries[it]) }
                )
            }
        }

        SectionLabel(tr("Detección", "Detection"), Modifier.padding(top = 6.dp))
        PulsoCard(modifier = Modifier.reveal(2), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            val presets = listOf(SensitivityPreset.BAJA, SensitivityPreset.MEDIA, SensitivityPreset.ALTA)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(tr("Sensibilidad", "Sensitivity"), style = PulsoType.label, color = c.onSurface)
                    Text(
                        when (preset) {
                            SensitivityPreset.BAJA -> tr("Menos avisos", "Fewer alerts")
                            SensitivityPreset.MEDIA -> tr("Recomendada", "Recommended")
                            SensitivityPreset.ALTA -> tr("Más avisos", "More alerts")
                            SensitivityPreset.PERSONALIZADO -> tr("Personalizada", "Custom")
                        },
                        style = PulsoType.caption, color = c.muted
                    )
                }
                SegmentedControl(
                    options = listOf(tr("Baja", "Low"), tr("Media", "Medium"), tr("Alta", "High")),
                    selectedIndex = presets.indexOf(preset),
                    onSelect = { onPreset(presets[it]); onSave() },
                    fill = true,
                    segmentHeight = 36.dp,
                    textStyle = PulsoType.chip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThresholdTile(tr("Pulso sobre la base", "Pulse above baseline"), "+" + "%.1f σ".format(config.hrZThreshold), c.heart, c.heartSoft, Modifier.weight(1f))
                ThresholdTile(tr("Caída de HRV", "HRV drop"), "%.1f σ".format(config.hrvZThreshold).replace("-", "−"), c.hrv, c.hrvSoft, Modifier.weight(1f))
            }
            if (qualityMetrics.totalInterventions > 0) {
                Text(
                    tr(
                        "Últimos 30 días: ${qualityMetrics.totalInterventions} avisos · ${qualityMetrics.accuratePct.roundToInt()} % acertados · ${qualityMetrics.falseAlarmPct.roundToInt()} % falsas alarmas",
                        "Last 30 days: ${qualityMetrics.totalInterventions} alerts · ${qualityMetrics.accuratePct.roundToInt()}% accurate · ${qualityMetrics.falseAlarmPct.roundToInt()}% false alarms"
                    ),
                    style = PulsoType.caption, color = c.muted
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showAdvanced = !showAdvanced }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("Umbrales personalizados", "Custom thresholds"), style = PulsoType.chip, color = c.primary, modifier = Modifier.weight(1f))
                val chevron by androidx.compose.animation.core.animateFloatAsState(if (showAdvanced) 90f else 0f, label = "chevron")
                Icon(PulsoIcons.ChevronRight, null, tint = c.primary, modifier = Modifier.size(18.dp).rotate(chevron))
            }
            AnimatedVisibility(showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThresholdSlider(tr("Elevación de pulso", "Pulse rise"), "+%.1f σ".format(config.hrZThreshold), config.hrZThreshold.toFloat(), 0.5f..2.5f, 19) {
                        update(config.copy(hrZThreshold = (it * 10).roundToInt() / 10.0))
                    }
                    ThresholdSlider(tr("Caída de HRV", "HRV drop"), "%.1f σ".format(config.hrvZThreshold), config.hrvZThreshold.toFloat(), -2.5f..-0.5f, 19) {
                        update(config.copy(hrvZThreshold = (it * 10).roundToInt() / 10.0))
                    }
                    ThresholdSlider(
                        tr("Ventanas seguidas", "Consecutive windows"),
                        tr("${config.consecutiveWindowsRuleA} (~${config.consecutiveWindowsRuleA * 5} min)", "${config.consecutiveWindowsRuleA} (~${config.consecutiveWindowsRuleA * 5} min)"),
                        config.consecutiveWindowsRuleA.toFloat(), 2f..6f, 3
                    ) { update(config.copy(consecutiveWindowsRuleA = it.roundToInt())) }
                    ThresholdSlider(tr("Espera entre avisos", "Wait between alerts"), "${config.cooldownMinutes} min", config.cooldownMinutes.toFloat(), 20f..180f, 15) {
                        update(config.copy(cooldownMinutes = it.roundToInt()))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        PulsoButton(tr("Restablecer", "Reset"), onResetDefaults, Modifier.weight(1f), style = PulsoButtonStyle.Ghost, height = 44.dp, textStyle = PulsoType.chip)
                        PulsoButton(tr("Guardar", "Save"), onSave, Modifier.weight(1f), height = 44.dp, loading = isSaving, textStyle = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
            }
        }

        adaptiveSuggestion?.let { s ->
            PulsoCard(background = c.hrvSoft, border = null, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(PulsoIcons.Spark, null, tint = c.hrv, modifier = Modifier.size(20.dp))
                    Text(
                        tr(
                            "Marcaste ${s.consecutiveNotStressedCount} avisos seguidos como “no era tensión”. ¿Bajamos la sensibilidad?",
                            "You marked ${s.consecutiveNotStressedCount} alerts in a row as “not tension”. Lower the sensitivity?"
                        ),
                        style = PulsoType.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulsoButton(tr("Aplicar", "Apply"), onApplySuggestion, Modifier.weight(1f), height = 40.dp, textStyle = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold))
                    PulsoButton(tr("Ahora no", "Not now"), onDismissSuggestion, Modifier.weight(1f), style = PulsoButtonStyle.Ghost, height = 40.dp, textStyle = PulsoType.chip)
                }
            }
        }

        SectionLabel(tr("Avisos", "Alerts"), Modifier.padding(top = 6.dp))
        PulsoCard(modifier = Modifier.reveal(3), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            ListRow(
                label = tr("Horas de silencio", "Quiet hours"),
                subtitle = fmt(quietHours.startHour, quietHours.startMinute) + " – " + fmt(quietHours.endHour, quietHours.endMinute),
                onClick = {
                    val i = QUIET_PRESETS.indexOfFirst { it.startHour == quietHours.startHour && it.endHour == quietHours.endHour }
                    onQuietHours(QUIET_PRESETS[(i + 1).mod(QUIET_PRESETS.size)])
                    onSave()
                }
            ) {
                Text(tr("Cambiar", "Change"), style = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold), color = c.primary)
            }
            val snooze = config.snoozeUntil
            if (snooze != null && snooze > System.currentTimeMillis()) {
                val mins = ((snooze - System.currentTimeMillis()) / 60_000).coerceAtLeast(1)
                ListRow(
                    label = tr("Avisos en pausa", "Alerts paused"),
                    subtitle = tr("Quedan $mins min", "$mins min left"),
                    onClick = onCancelSnooze
                ) {
                    Text(tr("Reanudar", "Resume"), style = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold), color = c.warn)
                }
            }
            ListRow(
                label = tr("Límite diario de avisos", "Daily alert limit"),
                subtitle = tr("Después esperan a mañana", "Then they wait until tomorrow"),
                showDivider = false
            ) {
                Stepper(
                    value = config.dailyCap,
                    onDecrement = { update(config.copy(dailyCap = (config.dailyCap - 1).coerceAtLeast(1))); onSave() },
                    onIncrement = { update(config.copy(dailyCap = (config.dailyCap + 1).coerceAtMost(20))); onSave() }
                )
            }
        }
        FeedbackText(saveFeedback, positive = saveFeedback?.contains("exitos") == true)

        SectionLabel(tr("Avanzado", "Advanced"), Modifier.padding(top = 6.dp))
        PulsoCard(modifier = Modifier.reveal(4), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            ListRow(tr("Línea base", "Baseline"), icon = PulsoIcons.Target, onClick = onOpenBaseline, minHeight = 54.dp) { Chevron() }
            ListRow(tr("Laboratorio · replay", "Lab · replay detection"), icon = PulsoIcons.Flask, onClick = onOpenLab, minHeight = 54.dp) { Chevron() }
            ListRow(tr("Diagnóstico del reloj", "Watch diagnostics"), icon = PulsoIcons.Watch, onClick = onOpenDiagnostics, minHeight = 54.dp, showDivider = false) { Chevron() }
        }

        PulsoButton(
            tr("Retirar consentimiento", "Withdraw consent"),
            { confirmWithdraw = true },
            Modifier.fillMaxWidth().padding(top = 4.dp),
            style = PulsoButtonStyle.DangerGhost,
            height = 48.dp,
            textStyle = PulsoType.button.copy(fontSize = 14.sp)
        )
    }

    if (confirmSignOut) {
        PulsoConfirmSheet(
            icon = PulsoIcons.Shield,
            title = tr("¿Cerrar sesión?", "Sign out?"),
            body = tr(
                "Tendrás que volver a entrar con tu correo y contraseña. Tus mediciones se quedan en este celular.",
                "You’ll need your e-mail and password to sign in again. Your readings stay on this phone."
            ),
            confirmLabel = tr("Cerrar sesión", "Sign out"),
            onConfirm = { confirmSignOut = false; onSignOut() },
            onDismiss = { confirmSignOut = false }
        )
    }

    if (confirmWithdraw) {
        PulsoConfirmSheet(
            icon = PulsoIcons.Shield,
            title = tr("¿Retirar tu consentimiento?", "Withdraw your consent?"),
            body = tr(
                "El reloj dejará de medir tu pulso de inmediato. Puedes conservar tus datos en el celular o borrarlos ahora.",
                "The watch stops measuring your pulse right away. You can keep your data on this phone or delete it now."
            ),
            confirmLabel = tr("Retirar", "Withdraw"),
            onConfirm = { confirmWithdraw = false; onRevokeConsent() },
            onDismiss = { confirmWithdraw = false },
            secondaryLabel = tr("Retirar y borrar mis datos", "Withdraw and delete my data"),
            onSecondary = { confirmWithdraw = false; onWipeData() }
        )
    }
}

@Composable
private fun Chevron() {
    Icon(PulsoIcons.ChevronRight, null, tint = Pulso.colors.muted, modifier = Modifier.size(18.dp))
}

@Composable
private fun ThresholdTile(label: String, value: String, color: Color, soft: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(soft).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = PulsoType.caption.copy(fontWeight = FontWeight.Bold), color = Pulso.colors.muted)
        Text(value, style = PulsoType.number.copy(fontSize = 18.sp), color = color)
    }
}

@Composable
private fun ThresholdSlider(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    val c = Pulso.colors
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = PulsoType.bodySmall.copy(fontWeight = FontWeight.Bold), color = c.onSurface)
            Text(value, style = PulsoType.number.copy(fontSize = 13.sp), color = c.primary)
        }
        Slider(
            value = current.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surface2,
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
            )
        )
    }
}
