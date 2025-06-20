package com.tangem.core.ui.components.inputrow.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.DEFAULT_ANIMATION_DURATION

/**
 * Paste button with cross icon. Retrieves text from clipboard.
 * [Paste button](https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7853-33535&mode=design&t=6o23sqF8fDQdn4C5-4)
 *
 * @param isPasteButtonVisible is paste button visible
 * @param onClick action callback
 * @param modifier composable modifier
 */
@Composable
fun PasteButton(
    isPasteButtonVisible: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColorEnabled: Color = TangemTheme.colors.button.primary,
    backgroundColorDisabled: Color = TangemTheme.colors.button.secondary,
    textColor: Color = TangemTheme.colors.text.primary2,
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val isPasteEnabled = !clipboardManager.getText()?.text.isNullOrEmpty()
    val color = if (isPasteEnabled) {
        backgroundColorEnabled
    } else {
        backgroundColorDisabled
    }
    AnimatedVisibility(
        visible = isPasteButtonVisible,
        label = "Paste Button Visibility Animation",
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(DEFAULT_ANIMATION_DURATION)),
        modifier = modifier,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_paste),
            style = TangemTheme.typography.caption1,
            color = textColor,
            modifier = Modifier
                .background(
                    color = color,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .padding(
                    horizontal = TangemTheme.dimens.spacing12,
                    vertical = TangemTheme.dimens.spacing4,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(radius = TangemTheme.dimens.radius8),
                    enabled = isPasteEnabled,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick(
                            clipboardManager
                                .getText()
                                ?.toString()
                                .orEmpty(),
                        )
                    },
                ),
        )
    }
}

@Composable
fun CrossIcon(onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_close_24),
        tint = TangemTheme.colors.icon.informative,
        contentDescription = stringResourceSafe(R.string.common_close),
        modifier = modifier
            .size(TangemTheme.dimens.size24)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(radius = TangemTheme.dimens.radius12),
                onClick = { onClick("") },
            ),
    )
}

@Preview
@Composable
private fun PasteButtonPreview() {
    var isVisible by remember { mutableStateOf(true) }
    TangemThemePreview {
        PasteButton(
            isPasteButtonVisible = isVisible,
            onClick = { isVisible = !isVisible },
        )
    }
}