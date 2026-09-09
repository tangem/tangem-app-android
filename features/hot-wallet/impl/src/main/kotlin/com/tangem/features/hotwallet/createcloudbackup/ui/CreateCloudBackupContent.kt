package com.tangem.features.hotwallet.createcloudbackup.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckbox
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.cloudbackup.password.PasswordStrength
import com.tangem.domain.cloudbackup.password.PasswordStrengthEvaluator
import com.tangem.domain.cloudbackup.password.PasswordStrengthHint
import com.tangem.features.hotwallet.common.ui.CloudBackupPasswordField
import com.tangem.features.hotwallet.createcloudbackup.entity.CreateCloudBackupUM
import com.tangem.features.hotwallet.impl.R

private const val ACCOUNT_NAME = "Google Account"

private val ContentHorizontalPadding = 24.dp
private val FooterHorizontalPadding = 16.dp
private val ContentFooterGap = 20.dp

@Composable
private fun footerInsets(): WindowInsets = WindowInsets.ime.union(WindowInsets.navigationBars)

@Composable
internal fun CreateCloudBackupContent(state: CreateCloudBackupUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = { TopBar(state) },
    ) { contentPadding ->
        StepAnimatedContent(state = state, label = "CreateCloudBackupContent") { step ->
            when (step) {
                is CreateCloudBackupUM.Preparing -> PreparingScreen(contentPadding)
                is CreateCloudBackupUM.SetPassword -> SetPasswordScreen(step, contentPadding)
                is CreateCloudBackupUM.ConfirmPassword -> ConfirmPasswordScreen(step, contentPadding)
                is CreateCloudBackupUM.Completed -> CompletedScreen(step, contentPadding)
            }
        }
    }
}

/**
 * Slides the steps sideways in the direction the flow moves, so going forward and going back read
 * differently. Keyed by step, so typing inside a step never restarts the transition.
 */
@Composable
private fun StepAnimatedContent(
    state: CreateCloudBackupUM,
    label: String,
    content: @Composable (CreateCloudBackupUM) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it.stepOrder },
        transitionSpec = {
            val isForward = targetState.stepOrder >= initialState.stepOrder
            val direction = if (isForward) 1 else -1
            val enter = slideInHorizontally { width -> direction * width } + fadeIn()
            val exit = slideOutHorizontally { width -> -direction * width } + fadeOut()
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = label,
    ) { step -> content(step) }
}

private val CreateCloudBackupUM.stepOrder: Int
    get() = when (this) {
        is CreateCloudBackupUM.Preparing -> 0
        is CreateCloudBackupUM.SetPassword -> 1
        is CreateCloudBackupUM.ConfirmPassword -> 2
        is CreateCloudBackupUM.Completed -> 3
    }

/**
 * Both navigation slots always occupy a button-sized box, even when they hold no button: the bar keeps
 * its height and the centered title keeps its position while the back / close buttons come and go.
 */
@Composable
private fun TopBar(state: CreateCloudBackupUM) {
    val isCompleted = state is CreateCloudBackupUM.Completed
    val title = if (isCompleted) {
        stringResourceSafe(R.string.common_done)
    } else {
        stringResourceSafe(R.string.hw_cloud_backup_restore_navtitle)
    }

    TangemTopNavigation(
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        startButton = {
            NavigationSlot {
                if (state is CreateCloudBackupUM.ConfirmPassword) {
                    TangemButton.Back(onClick = state.onBackClick)
                }
            }
        },
        endButton = {
            NavigationSlot {
                if (!isCompleted) TangemButton.Close(onClick = state.onCloseClick)
            }
        },
        contentColumn = {
            Box(
                modifier = Modifier.height(TangemTopNavigation.ButtonSlotSize),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(targetState = title, label = "CreateCloudBackupTitle") { current ->
                    TangemNavigationText(text = current, role = TangemNavigationText.Role.Title)
                }
            }
        },
    )
}

@Composable
private fun NavigationSlot(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.size(TangemTopNavigation.ButtonSlotSize),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun Footer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FooterHorizontalPadding, vertical = 12.dp),
        content = content,
    )
}

@Composable
private fun PreparingScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader()
    }
}

@Composable
private fun SetPasswordScreen(
    state: CreateCloudBackupUM.SetPassword,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScrollableContent(
        contentPadding = contentPadding,
        modifier = modifier,
        button = {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.hw_cloud_backup_set_password_button),
                isEnabled = state.isContinueEnabled,
                onClick = state.onContinueClick,
            )
        },
    ) {
        TitleBlock(
            title = stringResourceSafe(R.string.hw_cloud_backup_set_password_title),
            description = stringResourceSafe(
                R.string.hw_cloud_backup_set_password_description,
                stringResourceSafe(R.string.hw_cloud_backup_service_name),
            ),
        )
        SpacerH(24.dp)
        CloudBackupPasswordField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            isVisible = state.isPasswordVisible,
            onToggleVisibility = state.onToggleVisibility,
            focusRequester = focusRequester,
            contentType = ContentType.NewPassword,
        )
        SpacerH(12.dp)
        StrengthIndicator(strength = state.strength, hint = state.hint)
    }
}

@Composable
private fun ConfirmPasswordScreen(
    state: CreateCloudBackupUM.ConfirmPassword,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val autofillManager = LocalAutofillManager.current

    ScrollableContent(
        contentPadding = contentPadding,
        modifier = modifier,
        button = {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_confirm),
                isEnabled = state.isConfirmEnabled && !state.isLoading,
                isLoading = state.isLoading,
                onClick = {
                    // FR-13: offers saving the freshly set password to the platform password manager,
                    // while both fields are still composed and the autofill session is open
                    autofillManager?.commit()
                    state.onConfirmClick()
                },
            )
        },
        bottomContent = {
            ConsentRow(
                checked = state.isConsentChecked,
                onCheckedChange = state.onConsentChange,
                enabled = !state.isLoading,
            )
        },
    ) {
        TitleBlock(
            title = stringResourceSafe(R.string.hw_cloud_backup_confirm_password_title),
            description = stringResourceSafe(
                R.string.hw_cloud_backup_confirm_password_description,
                stringResourceSafe(R.string.hw_cloud_backup_service_name),
            ),
        )
        SpacerH(24.dp)
        CloudBackupPasswordField(
            value = state.confirmPassword,
            onValueChange = state.onPasswordChange,
            isVisible = state.isPasswordVisible,
            onToggleVisibility = state.onToggleVisibility,
            errorText = resourceReference(R.string.hw_cloud_backup_passwords_dont_match)
                .takeIf { state.isMismatch },
            enabled = !state.isLoading,
            focusRequester = focusRequester,
            contentType = ContentType.NewPassword,
        )
    }
}

@Composable
private fun CompletedScreen(
    state: CreateCloudBackupUM.Completed,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = ContentHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_success_blue_76),
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(R.string.hw_cloud_backup_completed_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResourceSafe(R.string.hw_cloud_backup_completed_description),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Footer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(footerInsets()),
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_finish),
                onClick = state.onFinishClick,
            )
        }
    }
}

/**
 * [button] stays pinned, the rest scrolls above it. The weighted spacer holds [bottomContent] at the
 * bottom while the content fits and collapses once it doesn't, so nothing ever overlaps.
 */
@Composable
private fun ScrollableContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    button: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(footerInsets()),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            Column(modifier = Modifier.padding(horizontal = ContentHorizontalPadding)) {
                SpacerH(20.dp)
                content()
            }
            SpacerH(ContentFooterGap)
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.padding(horizontal = FooterHorizontalPadding),
                content = bottomContent,
            )
        }
        Footer { button() }
    }
}

@Composable
private fun TitleBlock(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        SpacerH(8.dp)
        Text(
            text = description,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
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
        color = TangemTheme.colors3.text.status.error,
        fraction = 1f / 3f,
        labelRes = R.string.hw_cloud_backup_strength_weak,
    )
    PasswordStrength.MEDIUM -> StrengthUiState(
        color = TangemTheme.colors3.text.status.warning,
        fraction = 2f / 3f,
        labelRes = R.string.hw_cloud_backup_strength_medium,
    )
    PasswordStrength.STRONG -> StrengthUiState(
        color = TangemTheme.colors3.text.status.success,
        fraction = 1f,
        labelRes = R.string.hw_cloud_backup_strength_strong,
    )
    null -> StrengthUiState(
        color = TangemTheme.colors3.text.secondary,
        fraction = 0f,
        labelRes = R.string.hw_cloud_backup_strength_none,
    )
}

@Composable
private fun PasswordStrengthHint.hintText(): String = when (this) {
    PasswordStrengthHint.USE_ALL_CRITERIA -> stringResourceSafe(
        R.string.hw_cloud_backup_password_rule_v2,
        PasswordStrengthEvaluator.MIN_LENGTH,
    )
    PasswordStrengthHint.KEEP_GOING -> stringResourceSafe(
        R.string.hw_cloud_backup_strength_hint_keep_going_v2,
        PasswordStrengthEvaluator.MIN_LENGTH,
    )
    PasswordStrengthHint.ADD_SYMBOL -> stringResourceSafe(R.string.hw_cloud_backup_strength_hint_symbol)
    PasswordStrengthHint.ADD_NUMBER -> stringResourceSafe(R.string.hw_cloud_backup_strength_hint_number)
    PasswordStrengthHint.ADD_UPPERCASE -> stringResourceSafe(R.string.hw_cloud_backup_strength_hint_uppercase)
    PasswordStrengthHint.ADD_LOWERCASE -> stringResourceSafe(R.string.hw_cloud_backup_strength_hint_lowercase)
    PasswordStrengthHint.ALMOST_LONG -> stringResourceSafe(R.string.hw_cloud_backup_strength_hint_almost)
    PasswordStrengthHint.STRONG -> stringResourceSafe(R.string.hw_cloud_backup_strength_hint_ok)
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

    val trackColor = TangemTheme.colors3.border.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // FR-09: a password too short to rate shows no meter, only the neutral label and the hint.
            // The gap to the label lives inside the animated block, so it collapses together with the meter
            // instead of leaving the label offset until the animation ends.
            AnimatedVisibility(visible = strength != null) {
                Canvas(
                    modifier = Modifier
                        .padding(end = 4.dp)
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
                style = TangemTheme.typography3.subheading.medium,
                color = animatedColor,
            )
        }
        SpacerH(8.dp)
        Text(
            text = hint.hintText(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
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
            .padding(horizontal = 8.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.Top,
    ) {
        TangemCheckbox(
            modifier = Modifier
                .padding(top = 4.dp),
            checked = checked,
            onCheckedChange = onCheckedChange,
            isEnabled = enabled,
        )
        Text(
            text = stringResourceSafe(R.string.hw_cloud_backup_consent_v2, ACCOUNT_NAME),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSetPassword() {
    TangemThemePreviewRedesign {
        CreateCloudBackupContent(
            state = CreateCloudBackupUM.SetPassword(
                onBackClick = {},
                onCloseClick = {},
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
    TangemThemePreviewRedesign {
        CreateCloudBackupContent(
            state = CreateCloudBackupUM.ConfirmPassword(
                onBackClick = {},
                onCloseClick = {},
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
    TangemThemePreviewRedesign {
        CreateCloudBackupContent(
            state = CreateCloudBackupUM.Completed(onFinishClick = {}),
        )
    }
}