@file:Suppress("MagicNumber")

package com.tangem.features.storiesv2.impl.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.storiesv2.impl.ui.state.StoriesV2UM
import kotlinx.coroutines.launch

/**
 * The story screen.
 *
 * Two things here are load-bearing: the frame loop pulls the player once per displayed frame, and the gesture layer
 * sits *under* the chrome, so the close button and the actions take their own touches while every other pixel
 * belongs to the story.
 */
@Composable
internal fun StoriesV2Screen(
    state: StoriesV2UM,
    handle: StoryPlayerHandle,
    videoAspectRatio: Float?,
    modifier: Modifier = Modifier,
) {
    SystemBarsIconsDisposable(darkIcons = false)

    // A story is dark whatever the app is set to: the design's white primary button and translucent secondary one
    // are the dark-theme tokens, so following the app theme would put a black button on a black video.
    CompositionLocalProvider(LocalIsInDarkTheme provides true) {
        TangemThemeRedesign {
            StoryContent(
                state = state,
                handle = handle,
                videoAspectRatio = videoAspectRatio,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun StoryContent(
    state: StoriesV2UM,
    handle: StoryPlayerHandle,
    videoAspectRatio: Float?,
    modifier: Modifier = Modifier,
) {
    // A story asks nothing of the viewer, so the display would time out part way into the second video.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(handle) {
        while (true) {
            withFrameNanos(handle::onFrame)
        }
    }

    val scope = rememberCoroutineScope()
    val dismissOffset = remember { Animatable(0f) }
    val dismissThreshold = with(LocalDensity.current) { DISMISS_THRESHOLD.toPx() }

    // The gesture handlers are installed once and must not be reinstalled per slide — that would cancel a gesture
    // in flight — so the callbacks are read through a state that the current slide keeps up to date.
    val onHoldChange by rememberUpdatedState(state.onHoldChange)
    val onDragChange by rememberUpdatedState(state.onDragChange)
    val onTapBack by rememberUpdatedState(state.onTapBack)
    val onTapForward by rememberUpdatedState(state.onTapForward)
    val onSwipeDown by rememberUpdatedState(state.onSwipeDown)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Released in a `finally` on the handler itself, not only in the gesture's end callbacks: Compose
                // restarts a pointer input handler outright when the node is reset — a display or font scale change,
                // a fold, split screen — and a gesture in flight then never reaches them.
                fun release() {
                    onDragChange(false)
                    scope.launch { dismissOffset.animateTo(0f) }
                }
                try {
                    detectVerticalDragGestures(
                        onDragStart = { onDragChange(true) },
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                dismissOffset.snapTo((dismissOffset.value + dragAmount).coerceAtLeast(0f))
                            }
                        },
                        onDragEnd = {
                            if (dismissOffset.value > dismissThreshold) onSwipeDown() else release()
                        },
                        onDragCancel = ::release,
                    )
                } finally {
                    release()
                }
            }
            .pointerInput(Unit) {
                try {
                    detectStoryTaps(
                        onHoldChange = { isHeld -> onHoldChange(isHeld) },
                        onTapBack = { onTapBack() },
                        onTapForward = { onTapForward() },
                    )
                } finally {
                    onHoldChange(false)
                }
            },
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = dismissOffset.value }) {
            StorySlideContent(slide = state.slide, handle = handle, videoAspectRatio = videoAspectRatio)

            StoryChrome(state = state, handle = handle)
        }
    }
}

@Composable
private fun StoryChrome(state: StoriesV2UM, handle: StoryPlayerHandle) {
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val durationMs = state.slide.durationMs
            StoryProgressBar(
                slideCount = state.slideCount,
                slideIndex = state.slideIndex,
                position = {
                    if (durationMs <= 0L) 0f else handle.slidePositionMs.longValue.toFloat() / durationMs
                },
                // The strip stays centred, so the space the close button occupies is reserved on both sides:
                // a story long enough to reach it shrinks its segments instead of sliding under it.
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 52.dp),
            )
            TangemButton.Close(onClick = state.onCloseClick, modifier = Modifier.align(Alignment.CenterEnd))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.slide.title?.let { title ->
                Text(
                    text = title.resolveReference(),
                    style = TangemTheme.typography3.heading.medium,
                    color = TangemTheme.colors3.text.staticDark.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            state.slide.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.staticDark.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            if (state.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Secondary above primary, as the design system places them — not in composition order.
                    state.actions.sortedBy { it.isPrimary }.forEach { action ->
                        TangemButton(
                            text = action.label,
                            variant = if (action.isPrimary) {
                                TangemButton.Variant.Primary
                            } else {
                                TangemButton.Variant.Secondary
                            },
                            size = TangemButton.Size.X12,
                            iconEnd = action.iconRes?.let { TangemIconUM.Icon(iconRes = it) },
                            onClick = action.onClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tap zones and hold-to-pause.
 *
 * Pausing waits for [HOLD_DELAY_MS] rather than firing on touch down: pausing and resuming a hardware decoder for
 * the hundred milliseconds a tap takes hitches exactly the interaction that is supposed to be instant.
 */
private suspend fun PointerInputScope.detectStoryTaps(
    onHoldChange: (Boolean) -> Unit,
    onTapBack: () -> Unit,
    onTapForward: () -> Unit,
) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false)

    // A list rather than the change itself: a timeout and a cancelled gesture would both arrive as a null change,
    // and they mean opposite things here.
    val quickRelease = withTimeoutOrNull(HOLD_DELAY_MS) {
        listOfNotNull(waitForUpOrCancellation())
    }

    when {
        quickRelease == null -> {
            onHoldChange(true)
            // `awaitEachGesture` swallows a mid-gesture cancellation and restarts the block, so without this a
            // pointer stream reset under the finger would leave the story paused for good.
            try {
                waitForUpOrCancellation()
            } finally {
                onHoldChange(false)
            }
        }
        quickRelease.isEmpty() -> Unit
        else -> {
            val x = quickRelease.first().position.x
            if (x < size.width * BACK_ZONE_FRACTION) onTapBack() else onTapForward()
        }
    }
}

private const val HOLD_DELAY_MS = 200L
private const val BACK_ZONE_FRACTION = 1f / 3f
private val DISMISS_THRESHOLD = 120.dp