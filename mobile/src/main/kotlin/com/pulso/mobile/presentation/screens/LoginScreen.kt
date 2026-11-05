package com.pulso.mobile.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.mobile.data.auth.AuthRepository
import com.pulso.mobile.data.auth.AuthResult
import com.pulso.mobile.presentation.components.IconBadge
import com.pulso.mobile.presentation.components.IconCircleButton
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoLogo
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    auth: AuthRepository,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit
) {
    val c = Pulso.colors
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = sign in, 1 = sign up
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var slowServer by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val signUp = mode == 1

    LaunchedEffect(loading) {
        slowServer = false
        if (loading) {
            delay(6_000)
            slowServer = true
        }
    }

    val messages = mapOf(
        "INVALID_CREDENTIALS" to tr("Correo o contraseña incorrectos.", "Wrong e-mail or password."),
        "EMAIL_TAKEN" to tr("Ya existe una cuenta con ese correo. Inicia sesión.", "An account with that e-mail already exists. Sign in instead."),
        AuthRepository.ERROR_NETWORK to tr("Sin conexión con el servidor. Revisa tu internet.", "Can’t reach the server. Check your internet connection."),
        AuthRepository.ERROR_TIMEOUT to tr("El servidor tardó demasiado. Inténtalo de nuevo.", "The server took too long. Please try again."),
        AuthRepository.ERROR_NO_SERVER to tr(
            "Servidor no configurado: esta versión de la app se compiló sin PULSO_API_URL.",
            "Server not configured: this build of the app has no PULSO_API_URL."
        )
    )
    val genericError = tr("No se pudo continuar. Inténtalo de nuevo.", "Something went wrong. Please try again.")
    val validationError = when {
        signUp && name.trim().length < 2 -> tr("Escribe tu nombre.", "Enter your name.")
        !email.contains("@") || !email.contains(".") -> tr("Escribe un correo válido.", "Enter a valid e-mail.")
        signUp && password.length < 8 -> tr("La contraseña debe tener al menos 8 caracteres.", "Password must be at least 8 characters.")
        password.isEmpty() -> tr("Escribe tu contraseña.", "Enter your password.")
        else -> null
    }

    val submit: () -> Unit = submit@{
        if (loading) return@submit
        if (validationError != null) {
            error = validationError
            return@submit
        }
        focus.clearFocus()
        error = null
        loading = true
        scope.launch {
            val result = if (signUp) auth.register(name, email, password) else auth.login(email, password)
            loading = false
            if (result is AuthResult.Failure) {
                error = messages[result.errorCode] ?: result.serverMessage ?: genericError
            }
            // On success the session flow changes and the app moves on by itself.
        }
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            PulsoLogo(32.dp)
            Spacer(Modifier.size(10.dp))
            Text("Pulso", style = PulsoType.h2.copy(fontSize = 18.sp), color = c.onSurface, modifier = Modifier.weight(1f))
            SegmentedControl(options = listOf("ES", "EN"), selectedIndex = language.ordinal, onSelect = { onLanguage(AppLanguage.entries[it]) }, textStyle = PulsoType.chip)
            Spacer(Modifier.size(8.dp))
            IconCircleButton(if (c.isLight) PulsoIcons.Moon else PulsoIcons.Sun, tr("Cambiar tema", "Switch theme"), onToggleTheme)
        }

        Column(Modifier.reveal(0).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (signUp) tr("Crea tu cuenta", "Create your account") else tr("Hola de nuevo", "Welcome back"),
                style = PulsoType.screenTitle.copy(fontSize = 32.sp, lineHeight = 36.sp), color = c.onSurface
            )
            Text(
                if (signUp) tr("Tu cuenta guarda tu acceso a Pulso en cualquier celular.", "Your account keeps your Pulso access on any phone.")
                else tr("Inicia sesión para seguir cuidando tu calma.", "Sign in to keep looking after your calm."),
                style = PulsoType.body, color = c.muted
            )
        }

        SegmentedControl(
            options = listOf(tr("Iniciar sesión", "Sign in"), tr("Crear cuenta", "Sign up")),
            selectedIndex = mode,
            onSelect = { mode = it; error = null },
            fill = true,
            segmentHeight = 40.dp,
            textStyle = PulsoType.chip.copy(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().reveal(1)
        )

        PulsoCard(modifier = Modifier.reveal(2), radius = 24.dp, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedVisibility(signUp, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                AuthField(
                    label = tr("Nombre", "Name"),
                    value = name,
                    onChange = { name = it; error = null },
                    keyboardType = KeyboardType.Text,
                    onNext = { focus.moveFocus(FocusDirection.Down) }
                )
            }
            AuthField(
                label = tr("Correo", "E-mail"),
                value = email,
                onChange = { email = it; error = null },
                keyboardType = KeyboardType.Email,
                onNext = { focus.moveFocus(FocusDirection.Down) }
            )
            AuthField(
                label = tr("Contraseña", "Password"),
                value = password,
                onChange = { password = it; error = null },
                keyboardType = KeyboardType.Password,
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword },
                imeAction = ImeAction.Done,
                onDone = submit
            )
            if (signUp) {
                Text(tr("Mínimo 8 caracteres.", "At least 8 characters."), style = PulsoType.caption, color = c.muted)
            }
        }

        AnimatedVisibility(error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.heartSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(PulsoIcons.Info, null, tint = c.heart, modifier = Modifier.size(20.dp))
                Text(error.orEmpty(), style = PulsoType.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.onSurface)
            }
        }

        PulsoButton(
            if (signUp) tr("Crear cuenta", "Create account") else tr("Entrar", "Sign in"),
            submit,
            Modifier.fillMaxWidth().reveal(3),
            height = 56.dp,
            loading = loading,
            trailingIcon = if (loading) null else PulsoIcons.ArrowRight,
            textStyle = PulsoType.button.copy(fontSize = 16.sp)
        )

        AnimatedVisibility(slowServer && loading, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.warnSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(PulsoIcons.Clock, null, tint = c.warn, modifier = Modifier.size(20.dp))
                Text(
                    tr("El servidor está despertando; la primera vez puede tardar hasta un minuto.", "The server is waking up; the first time can take up to a minute."),
                    style = PulsoType.bodySmall, color = c.onSurface
                )
            }
        }

        Row(Modifier.fillMaxWidth().reveal(4), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(PulsoIcons.Shield, c.primary, c.primarySoft, size = 34.dp, iconSize = 18.dp, radius = 11.dp)
            Text(
                tr("Tu contraseña se guarda cifrada. Tus mediciones siguen solo en tus equipos.", "Your password is stored encrypted. Your readings stay on your devices."),
                style = PulsoType.caption, color = c.muted, modifier = Modifier.weight(1f)
            )
        }
        Text(
            if (auth.isServerConfigured) auth.baseUrl.removePrefix("https://").removePrefix("http://")
            else tr("Servidor no configurado", "Server not configured"),
            style = PulsoType.caption.copy(fontSize = 11.sp), color = c.muted.copy(alpha = 0.7f),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val c = Pulso.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = PulsoType.chip, color = c.muted)
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(c.surface2)
                .border(1.dp, c.outline, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = PulsoType.body.copy(color = c.onSurface),
                cursorBrush = SolidColor(c.primary),
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
                modifier = Modifier.weight(1f).semantics { contentDescription = label }
            )
            if (isPassword) {
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onTogglePassword).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (showPassword) tr("Ocultar", "Hide") else tr("Ver", "Show"),
                        style = PulsoType.caption.copy(fontWeight = FontWeight.ExtraBold), color = c.primary
                    )
                }
            }
        }
    }
}
