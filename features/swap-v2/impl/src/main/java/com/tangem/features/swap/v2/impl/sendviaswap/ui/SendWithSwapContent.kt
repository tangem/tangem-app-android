package com.tangem.features.swap.v2.impl.sendviaswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute

@Composable
internal fun SendWithSwapContent(
    navigationUM: NavigationUM,
    stackState: ChildStack<SendWithSwapRoute, ComposableContentComponent>,
) {
    val navigationUM = navigationUM as? NavigationUM.Content ?: return

    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            text = navigationUM.title.resolveReference(),
            onBackClick = navigationUM.backIconClick,
            iconRes = navigationUM.additionalIconRes,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
        Children(
            stack = stackState,
            animation = stackAnimation { child ->
                when (child.configuration) {
                    SendWithSwapRoute.Confirm -> fade()
                    SendWithSwapRoute.Success -> slide(orientation = Orientation.Vertical) + fade()
                    else -> slide()
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            it.instance.Content(Modifier.weight(1f))
        }
        // TODO refactor [REDACTED_TASK_KEY]
        val primaryButton = navigationUM.primaryButton
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = primaryButton.textReference.resolveReference(),
                icon = primaryButton.iconRes?.let {
                    TangemButtonIconPosition.End(it)
                } ?: TangemButtonIconPosition.None,
                enabled = primaryButton.isEnabled,
                onClick = primaryButton.onClick,
                showProgress = false,
                colors = TangemButtonsDefaults.primaryButtonColors,
                textStyle = TangemTheme.typography.subtitle1,
            )
        }
    }
}