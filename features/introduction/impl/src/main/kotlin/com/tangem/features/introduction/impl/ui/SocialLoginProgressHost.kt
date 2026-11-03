package com.tangem.features.introduction.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM

private const val PROGRESS_FADE_MS = 200

@Composable
internal fun SocialLoginProgressHost(progress: SocialLoginProgressUM?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = progress,
        modifier = modifier.fillMaxSize(),
        contentKey = { it?.javaClass },
        transitionSpec = {
            fadeIn(tween(durationMillis = PROGRESS_FADE_MS)) togetherWith
                fadeOut(tween(durationMillis = PROGRESS_FADE_MS))
        },
        label = "SocialLoginProgressKind",
    ) { target ->
        target?.let { SocialLoginProgress(state = it) }
    }
}

/**
 * Swallows input and the back gesture on purpose: the buttons underneath stay composed, and leaving now
 * would drop the viewer back on the welcome screen while the wallet is still being written.
 */
@Composable
private fun SocialLoginProgress(state: SocialLoginProgressUM) {
    BackHandler(enabled = true) {}

    when (state) {
        SocialLoginProgressUM.Loader -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.overlay.modal)
                .consumeAllPointerInput(),
            contentAlignment = Alignment.Center,
        ) {
            TangemLoader(size = TangemLoaderSize.X32)
        }
        is SocialLoginProgressUM.Stage -> SocialLoginStageProgress(state)
    }
}

@Composable
private fun SocialLoginStageProgress(state: SocialLoginProgressUM.Stage) {
    val firstStage = remember { state }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // opaque, so the cross-fade between two stages never lets the welcome video show through
            .background(TangemTheme.colors3.bg.primary)
            .consumeAllPointerInput(),
    ) {
        // The status screen centres a block whose height depends on the text, so a longer title would
        // re-centre everything mid-read. The cross-fade hides the move; only the first stage animates in.
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(tween(durationMillis = PROGRESS_FADE_MS)) togetherWith
                    fadeOut(tween(durationMillis = PROGRESS_FADE_MS)) using SizeTransform(clip = false)
            },
            label = "SocialLoginProgress",
        ) { stage ->
            TangemStatusScreen(
                variant = TangemStatusScreen.Variant.Info,
                title = stage.title,
                description = stage.description,
                asset = { TangemLoader(size = TangemLoaderSize.X32, color = TangemTheme.colors3.icon.brand) },
                animated = stage == firstStage,
            )
        }
    }
}

private fun Modifier.consumeAllPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}