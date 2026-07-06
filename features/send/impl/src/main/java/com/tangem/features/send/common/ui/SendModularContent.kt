package com.tangem.features.send.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.common.CommonSendRoute

/**
 * Shared pull-based host for the regular Send and NFT Send flows (both over [CommonSendRoute]). Renders the
 * ACTIVE child's [ComposableModularContentComponent.Title] / [ComposableModularContentComponent.Footer] slots
 * in place (matching the previous in-place app-bar/footer behavior), while keeping the per-route slide/fade
 * Decompose [Children] animation for the Content region (and the bottom `Fade` gradient, hidden on
 * `ConfirmSuccess`, exactly as the previous `SendContent`).
 *
 * Each step's `Footer()` owns its own bottom block (Confirm reveals `SendingText`; Success/Empty render
 * nothing), so the host no longer special-cases routes for the footer.
 */
@Composable
internal fun SendModularContent(stackState: ChildStack<CommonSendRoute, ComposableModularContentComponent>) {
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
                    is CommonSendRoute.ConfirmSuccess -> fade(minAlpha = 1.0f)
                    is CommonSendRoute.Confirm -> fade()
                    else -> slide()
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                it.instance.Content(Modifier.fillMaxSize(1f))

                if (stackState.active.configuration != CommonSendRoute.ConfirmSuccess) {
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