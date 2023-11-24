package com.tangem.core.ui.components.inputrow.inner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * Paste button with cross icon. Retrieves text from clipboard.
 * [Paste button](https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7853-33535&mode=design&t=6o23sqF8fDQdn4C5-4)
 *
 * @param isPasteButtonVisible is paste button visible
 * @param onClick action callback
 * @param modifier composable modifier
 */
@Composable
fun PasteButton(isPasteButtonVisible: Boolean, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    if (isPasteButtonVisible) {
        Box(modifier = modifier) {
            Text(
                text = "Paste",
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.primary2,
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.button.primary,
                        shape = TangemTheme.shapes.roundedCornersXMedium,
                    )
                    .padding(
                        horizontal = TangemTheme.dimens.spacing10,
                        vertical = TangemTheme.dimens.spacing2,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(radius = TangemTheme.dimens.radius8),
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
    } else {
        Icon(
            painter = painterResource(id = R.drawable.ic_close_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = stringResource(R.string.common_close),
            modifier = modifier
                .size(TangemTheme.dimens.size20)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(radius = TangemTheme.dimens.radius10),
                    onClick = { onClick("") },
                ),
        )
    }
}
