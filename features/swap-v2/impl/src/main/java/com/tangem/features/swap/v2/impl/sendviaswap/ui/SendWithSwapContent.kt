package com.tangem.features.swap.v2.impl.sendviaswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute

@Composable
internal fun SendWithSwapContent(stackState: ChildStack<SendWithSwapRoute, ComposableModularContentComponent>) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        stackState.active.instance.Title()
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
            Box(modifier = Modifier.fillMaxHeight()) {
                it.instance.Content(Modifier.fillMaxSize(1f))

                if (stackState.active.configuration != SendWithSwapRoute.Success) {
                    Fade(
                        backgroundColor = TangemTheme.colors.background.tertiary,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        stackState.active.instance.Footer()
    }
}