package com.tangem.common.ui.navigationButtons

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.navigationButtons.preview.NavigationButtonsPreview
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList

@Composable
fun NavigationButtonsBlock(
    buttonState: NavigationButtonsState,
    modifier: Modifier = Modifier,
    footerText: TextReference? = null,
) {
    val state = buttonState as? NavigationButtonsState.Data
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        InfoText(footerText)
        ExtraButtons(state?.extraButtons, state?.txUrl)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            PreviousButton(state?.prevButton)
            NavigationPrimaryButton(state?.primaryButton, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun NavigationPrimaryButton(primaryButton: NavigationButton?, modifier: Modifier = Modifier) {
    val wrappedButton by rememberNavigationButton(primaryButton)
    AnimatedContent(
        targetState = wrappedButton,
        transitionSpec = { navigationButtonsTransition() },
        contentAlignment = Alignment.Center,
        label = "Animate show primary button",
        modifier = modifier.fillMaxWidth(),
    ) { button ->
        if (button != null && button.textReference != TextReference.EMPTY) {
            val icon = if (button.iconRes != null && button.isIconVisible) {
                TangemButtonIconPosition.End(iconResId = button.iconRes)
            } else {
                TangemButtonIconPosition.None
            }
            TangemButton(
                text = button.textReference.resolveReference(),
                enabled = button.isEnabled,
                onClick = button.onClick,
                showProgress = button.showProgress,
                colors = TangemButtonsDefaults.primaryButtonColors,
                textStyle = TangemTheme.typography.subtitle1,
                icon = icon,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PreviousButton(prevButton: NavigationButton?) {
    AnimatedVisibility(
        visible = prevButton != null,
        enter = expandHorizontally(expandFrom = Alignment.End),
        exit = shrinkHorizontally(shrinkTowards = Alignment.End),
        label = "Animate show prev button",
    ) {
        val button = remember(this) { requireNotNull(prevButton) }
        if (button.iconRes != null && button.isIconVisible) {
            Icon(
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(button.iconRes),
                ),
                tint = TangemTheme.colors.icon.primary1,
                contentDescription = null,
                modifier = Modifier
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                    .background(TangemTheme.colors.button.secondary)
                    .clickable(onClick = button.onClick)
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun ExtraButtons(extraButtons: ImmutableList<NavigationButton>?, txUrl: String?) {
    AnimatedVisibility(
        visible = !txUrl.isNullOrBlank() && extraButtons != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }).plus(fadeIn()),
        exit = slideOutVertically(targetOffsetY = { it / 2 }).plus(fadeOut()),
        label = "Animate show sent state buttons",
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buttons = remember(this) { requireNotNull(extraButtons) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
        ) {
            buttons.forEach { button ->
                val icon = button.iconRes?.let { TangemButtonIconPosition.Start(iconResId = it) }
                    ?: TangemButtonIconPosition.None
                TangemButton(
                    text = button.textReference.resolveReference(),
                    icon = icon,
                    textStyle = TangemTheme.typography.subtitle1,
                    onClick = rememberHapticFeedback(state = button, onAction = button.onClick),
                    modifier = Modifier.weight(1f),
                    enabled = button.isEnabled,
                    showProgress = false,
                    colors = TangemButtonsDefaults.secondaryButtonColors,
                )
            }
        }
    }
}

@Composable
private fun InfoText(footerText: TextReference?, modifier: Modifier = Modifier) {
    var isVisibleProxy by remember { mutableStateOf(!footerText.isNullOrEmpty()) }
    val keyboard by keyboardAsState()

    // the text should appear when the keyboard is closed
    LaunchedEffect(footerText, keyboard) {
        if (footerText.isNullOrEmpty() && keyboard is Keyboard.Opened) {
            return@LaunchedEffect
        }
        isVisibleProxy = !footerText.isNullOrEmpty()
    }

    AnimatedVisibility(
        visible = isVisibleProxy,
        modifier = modifier,
        enter = slideInVertically() + fadeIn(),
        exit = fadeOut(tween(durationMillis = 300)),
        label = "Animate footer text appearance",
    ) {
        val text = remember(this) { requireNotNull(footerText) }
        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TangemTheme.dimens.spacing12),
        )
    }
}

@Composable
private fun rememberNavigationButton(button: NavigationButton?): MutableState<NavigationButton?> {
    return remember(
        button?.iconRes,
        button?.isIconVisible,
        button?.isEnabled,
        button?.showProgress,
        button?.textReference,
    ) { mutableStateOf(button) }
}

private fun <T> AnimatedContentTransitionScope<T>.navigationButtonsTransition(): ContentTransform {
    val isPrimaryToHide = targetState != null && initialState == null
    val isPrimaryWasVisible = targetState == null && initialState != null
    return if (isPrimaryToHide || isPrimaryWasVisible) {
        slideInVertically(initialOffsetY = { it / 2 }).plus(fadeIn())
            .togetherWith(slideOutVertically(targetOffsetY = { it / 2 }).plus(fadeOut()))
    } else {
        fadeIn().togetherWith(fadeOut())
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationButtonsBlock_Preview(
    @PreviewParameter(NavigationButtonsBlockDataProvider::class) navigationButtonsState: NavigationButtonsState,
) {
    TangemThemePreview {
        NavigationButtonsBlock(navigationButtonsState)
    }
}

private class NavigationButtonsBlockDataProvider : PreviewParameterProvider<NavigationButtonsState> {
    override val values: Sequence<NavigationButtonsState>
        get() = sequenceOf(NavigationButtonsPreview.allButtons)
}

// endregion
