package com.tangem.features.hotwallet.createcloudbackup.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.ds2.checkbox.TangemCheckbox
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.cloudbackup.password.PasswordStrength
import com.tangem.domain.cloudbackup.password.PasswordStrengthHint
import com.tangem.features.hotwallet.common.ui.CloudBackupPasswordField
import com.tangem.features.hotwallet.createcloudbackup.entity.CreateCloudBackupUM
import com.tangem.features.hotwallet.impl.R

@Composable
internal fun CreateCloudBackupContent(state: CreateCloudBackupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
    ) {
        if (state !is CreateCloudBackupUM.Completed) {
            TangemTopAppBar(
                startButton = TopAppBarButtonUM.Back(onBackClicked = state.onBackClick),
                title = stringResourceSafe(R.string.common_backup),
            )
        }

        when (state) {
            is CreateCloudBackupUM.SetPassword -> SetPasswordScreen(state, Modifier.weight(1f))
            is CreateCloudBackupUM.ConfirmPassword -> ConfirmPasswordScreen(state, Modifier.weight(1f))
            is CreateCloudBackupUM.Completed -> CompletedScreen(state, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SetPasswordScreen(state: CreateCloudBackupUM.SetPassword, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleBlock(
                title = stringResourceSafe(R.string.hw_cloud_backup_set_password_title),
                description = stringResourceSafe(
                    R.string.hw_cloud_backup_set_password_description,
                    stringResourceSafe(R.string.hw_cloud_backup_service_name),
                ),
                modifier = Modifier.padding(top = 20.dp),
            )
            CloudBackupPasswordField(
                value = state.password,
                onValueChange = state.onPasswordChange,
                isVisible = state.isPasswordVisible,
                onToggleVisibility = state.onToggleVisibility,
                isError = false,
                contentType = ContentType.NewPassword,
            )
            StrengthIndicator(strength = state.strength, hint = state.hint)
        }
        PrimaryButton(
            text = stringResourceSafe(R.string.hw_cloud_backup_set_password_button),
            onClick = state.onContinueClick,
            enabled = state.isContinueEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun ConfirmPasswordScreen(state: CreateCloudBackupUM.ConfirmPassword, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val autofillManager = LocalAutofillManager.current

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleBlock(
                title = stringResourceSafe(R.string.hw_cloud_backup_confirm_password_title),
                description = stringResourceSafe(
                    R.string.hw_cloud_backup_confirm_password_description,
                    stringResourceSafe(R.string.hw_cloud_backup_service_name),
                ),
                modifier = Modifier.padding(top = 20.dp),
            )
            CloudBackupPasswordField(
                value = state.confirmPassword,
                onValueChange = state.onPasswordChange,
                isVisible = state.isPasswordVisible,
                onToggleVisibility = state.onToggleVisibility,
                isError = state.isMismatch,
                enabled = !state.isLoading,
                focusRequester = focusRequester,
                contentType = ContentType.NewPassword,
            )
            if (state.isMismatch) {
                Text(
                    text = stringResourceSafe(R.string.hw_cloud_backup_passwords_dont_match),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.warning,
                )
            }
        }
        ConsentRow(
            checked = state.isConsentChecked,
            onCheckedChange = state.onConsentChange,
            enabled = !state.isLoading,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        PrimaryButton(
            text = stringResourceSafe(R.string.common_confirm),
            onClick = {
                // FR-13: offers saving the freshly set password to the platform password manager,
                // while both fields are still composed and the autofill session is open
                autofillManager?.commit()
                state.onConfirmClick()
            },
            enabled = state.isConfirmEnabled && !state.isLoading,
            showProgress = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun CompletedScreen(state: CreateCloudBackupUM.Completed, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_success_blue_76),
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(R.string.hw_cloud_backup_completed_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResourceSafe(R.string.hw_cloud_backup_completed_description),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        PrimaryButton(
            text = stringResourceSafe(R.string.common_finish),
            onClick = state.onFinishClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun TitleBlock(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = description,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Immutable
private data class StrengthUiState(
    val color: Color,
    val fraction: Float,
    val labelRes: Int,
)

@Composable
private fun PasswordStrength?.toUiState(): StrengthUiState = when (this) {
    PasswordStrength.WEAK -> StrengthUiState(
        color = TangemTheme.colors.text.warning,
        fraction = 1f / 3f,
        labelRes = R.string.hw_cloud_backup_strength_weak,
    )
    PasswordStrength.MEDIUM -> StrengthUiState(
        color = TangemTheme.colors.text.attention,
        fraction = 2f / 3f,
        labelRes = R.string.hw_cloud_backup_strength_medium,
    )
    PasswordStrength.STRONG -> StrengthUiState(
        color = TangemTheme.colors.text.accent,
        fraction = 1f,
        labelRes = R.string.hw_cloud_backup_strength_strong,
    )
    null -> StrengthUiState(
        color = TangemTheme.colors.text.secondary,
        fraction = 0f,
        labelRes = R.string.hw_cloud_backup_strength_none,
    )
}

private fun PasswordStrengthHint.toHintRes(): Int = when (this) {
    PasswordStrengthHint.USE_ALL_CRITERIA -> R.string.hw_cloud_backup_password_rule
    PasswordStrengthHint.KEEP_GOING -> R.string.hw_cloud_backup_strength_hint_keep_going
    PasswordStrengthHint.ADD_SYMBOL -> R.string.hw_cloud_backup_strength_hint_symbol
    PasswordStrengthHint.ADD_NUMBER -> R.string.hw_cloud_backup_strength_hint_number
    PasswordStrengthHint.ADD_UPPERCASE -> R.string.hw_cloud_backup_strength_hint_uppercase
    PasswordStrengthHint.ADD_LOWERCASE -> R.string.hw_cloud_backup_strength_hint_lowercase
    PasswordStrengthHint.ALMOST_LONG -> R.string.hw_cloud_backup_strength_hint_almost
    PasswordStrengthHint.STRONG -> R.string.hw_cloud_backup_strength_hint_ok
}

@Composable
private fun StrengthIndicator(strength: PasswordStrength?, hint: PasswordStrengthHint, modifier: Modifier = Modifier) {
    val uiState = strength.toUiState()
    val animatedColor by animateColorAsState(targetValue = uiState.color, label = "strengthColor")

    // The meter keeps the last rated fill and colour while it animates away, so it slides out as it was
    // instead of first draining to the neutral state.
    var lastRated by remember { mutableStateOf<PasswordStrength?>(null) }
    if (strength != null) lastRated = strength
    val meterUiState = lastRated.toUiState()
    val meterColor by animateColorAsState(targetValue = meterUiState.color, label = "meterColor")
    val animatedFraction by animateFloatAsState(targetValue = meterUiState.fraction, label = "strengthFraction")

    val trackColor = TangemTheme.colors.icon.inactive

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // FR-09: a password too short to rate shows no meter, only the neutral label and the hint.
            // The gap to the label lives inside the animated block, so it collapses together with the meter
            // instead of leaving the label offset until the animation ends.
            AnimatedVisibility(visible = strength != null) {
                Canvas(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp),
                ) {
                    val stroke = 2.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    drawArc(
                        color = meterColor,
                        startAngle = -90f,
                        sweepAngle = animatedFraction * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                text = stringResourceSafe(uiState.labelRes),
                style = TangemTheme.typography.body2,
                color = animatedColor,
            )
        }
        Text(
            text = stringResourceSafe(hint.toHintRes()),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemCheckbox(checked = checked, onCheckedChange = onCheckedChange, isEnabled = enabled)
        Text(
            text = stringResourceSafe(R.string.hw_cloud_backup_consent),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSetPassword() {
    TangemThemePreview {
        CreateCloudBackupContent(
            state = CreateCloudBackupUM.SetPassword(
                onBackClick = {},
                password = "Str0ng!Pass",
                isPasswordVisible = false,
                strength = PasswordStrength.STRONG,
                hint = PasswordStrengthHint.STRONG,
                onPasswordChange = {},
                onToggleVisibility = {},
                onContinueClick = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConfirmPassword() {
    TangemThemePreview {
        CreateCloudBackupContent(
            state = CreateCloudBackupUM.ConfirmPassword(
                onBackClick = {},
                confirmPassword = "Str0ng!Pas",
                isPasswordVisible = false,
                isMismatch = true,
                isConsentChecked = false,
                isLoading = false,
                onPasswordChange = {},
                onToggleVisibility = {},
                onConsentChange = {},
                onConfirmClick = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCompleted() {
    TangemThemePreview {
        CreateCloudBackupContent(
            state = CreateCloudBackupUM.Completed(onFinishClick = {}),
        )
    }
}