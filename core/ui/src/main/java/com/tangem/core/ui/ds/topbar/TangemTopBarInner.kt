package com.tangem.core.ui.ds.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme

/**
 * Internal top bar composable that arranges optional start, center, and end content.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8435-74860&m=dev)
 *
 * @param modifier              Modifier to be applied to the top bar.
 * @param content               Center content of the top bar.
 * @param startContent          Optional start content of the top bar.
 * @param onStartContentClick   Optional click action for the start content.
 * @param endContent            Optional end content of the top bar.
 * @param onEndContentClick     Optional click action for the end content.
 * @param isGhostButtons        Flag to determine if ghost button styling should be applied.
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun TangemTopBarInner(
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
    startContent: (@Composable () -> Unit)? = null,
    onStartContentClick: (() -> Unit)? = null,
    endContent: (@Composable () -> Unit)? = null,
    onEndContentClick: (() -> Unit)? = null,
    isGhostButtons: Boolean = false,
) {
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(density = this).toDp() }
    Box(
        modifier = modifier
            .height(TangemTheme.dimens2.x16 + statusBarHeight)
            .fillMaxWidth()
            .padding(top = statusBarHeight)
            .padding(TangemTheme.dimens2.x4, TangemTheme.dimens2.x3),
    ) {
        val iconModifier = Modifier
            .size(TangemTheme.dimens2.x10)
            .clip(RoundedCornerShape(TangemTheme.dimens2.x25))
            .conditionalCompose(isGhostButtons) {
                background(TangemTheme.colors2.button.backgroundSecondary)
            }

        AnimatedVisibility(
            visible = startContent != null,
            modifier = Modifier.align(Alignment.CenterStart),
            label = "Start Content Visibility",
        ) {
            Box(
                modifier = iconModifier
                    .conditional(onStartContentClick != null) {
                        clickableSingle { onStartContentClick?.invoke() }
                    }
                    .conditionalCompose(isGhostButtons) { padding(TangemTheme.dimens2.x1) },
            ) {
                startContent?.invoke()
            }
        }

        AnimatedVisibility(
            visible = content != null,
            modifier = Modifier.align(Alignment.Center),
        ) {
            content?.invoke()
        }

        AnimatedVisibility(
            visible = endContent != null,
            modifier = Modifier.align(Alignment.CenterEnd),
            label = "End Content Visibility",
        ) {
            Box(
                modifier = iconModifier
                    .conditional(onEndContentClick != null) {
                        clickableSingle { onEndContentClick?.invoke() }
                    }
                    .conditionalCompose(isGhostButtons) { padding(TangemTheme.dimens2.x1) },
            ) {
                endContent?.invoke()
            }
        }
    }
}