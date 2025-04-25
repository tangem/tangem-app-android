package com.tangem.features.send.v2.common.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.features.send.v2.send.ui.state.ButtonsUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.common.ui.state.NavigationUM

@Composable
internal fun SendNavigationButtons(navigationUM: NavigationUM, modifier: Modifier = Modifier) {
    val navigationUM = navigationUM as? NavigationUM.Content ?: return

    Column(
        modifier = modifier.padding(
            start = TangemTheme.dimens.spacing16,
            end = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing16,
        ),
    ) {
        SendDoneButtons(navigationUM.secondaryPairButtonsUM)
        SendNavigationButton(
            navigationUM = navigationUM,
        )
    }
}

@Composable
private fun SendNavigationButton(navigationUM: NavigationUM, modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current
    val navigationUM = navigationUM as? NavigationUM.Content ?: return
    val primaryButton = navigationUM.primaryButton

    Row(modifier = modifier) {
        AnimatedVisibility(
            visible = navigationUM.prevButton != null,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            val wrappedNavigationUM = remember(this) { requireNotNull(navigationUM.prevButton) }
            Row {
                Icon(
                    painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_back_24)),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(TangemTheme.colors.button.secondary)
                        .clickable(onClick = wrappedNavigationUM.onClick)
                        .padding(12.dp),
                )
                SpacerW12()
            }
        }
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = primaryButton.text.resolveReference(),
            icon = primaryButton.iconResId?.let {
                TangemButtonIconPosition.End(it)
            } ?: TangemButtonIconPosition.None,
            enabled = primaryButton.isEnabled,
            onClick = {
                if (primaryButton.isHapticClick) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                primaryButton.onClick()
            },
            showProgress = false,
            colors = TangemButtonsDefaults.primaryButtonColors,
            textStyle = TangemTheme.typography.subtitle1,
        )
    }
}

@Composable
private fun SendDoneButtons(pairButtonsUM: ButtonsUM.SecondaryPairButtonsUM?, modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = pairButtonsUM != null,
        modifier = modifier,
        enter = slideInVertically().plus(fadeIn()),
        exit = slideOutVertically().plus(fadeOut()),
        label = "Animate show sent state buttons",
    ) {
        val wrappedPairButtonsUM = remember(this) { requireNotNull(pairButtonsUM) }
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            SecondaryButtonIconStart(
                text = wrappedPairButtonsUM.leftText.resolveReference(),
                iconResId = wrappedPairButtonsUM.leftIconResId!!,
                onClick = wrappedPairButtonsUM.onLeftClick,
                modifier = Modifier.weight(1f),
            )
            SpacerW12()
            SecondaryButtonIconStart(
                text = wrappedPairButtonsUM.rightText.resolveReference(),
                iconResId = wrappedPairButtonsUM.rightIconResId!!,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    wrappedPairButtonsUM.onRightClick()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}