package com.tangem.features.introduction.impl.ui

import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import com.tangem.core.res.R
import com.tangem.core.ui.components.KeepScreenOn
import com.tangem.core.ui.components.legal.LegalCaption
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.introduction.impl.model.welcomeScenes
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.features.introduction.impl.R as IntroductionR
import com.tangem.core.ui.R as CoreUiR

/** Vertical stops of the darkening gradient, each paired with the scrim opacity it reaches. */
private val ScrimGradient = listOf(
    0.52f to 0f,
    0.63f to 0.5f,
    0.70f to 0.8f,
    0.76f to 0.96f,
    0.82f to 1f,
    1f to 1f,
)

/** How long the warm-up pass waits for a frame nobody sees before taking one that someone might. */
private val PREWARM_COVER_TIMEOUT = 1.seconds

@Composable
internal fun IntroductionScreen(
    state: IntroductionUM,
    onSurfaceAvailable: (SurfaceView) -> Unit,
    onSurfaceRelease: (SurfaceView) -> Unit,
    videoPositionMs: () -> Long,
    modifier: Modifier = Modifier,
) {
    SystemBarsIconsDisposable(darkIcons = false)
    val isAnimated = state.isMotionEnabled && !state.isVideoFailed
    // Nothing moves under reduced motion or with the video down, so there is nothing to keep the display awake for.
    if (isAnimated) KeepScreenOn()

    // The design's colours are the dark-theme tokens, and re-entering the theme is what recomputes them.
    CompositionLocalProvider(LocalIsInDarkTheme provides true) {
        TangemThemeRedesign {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(TangemTheme.colors3.bg.base),
            ) {
                VideoBackground(
                    state = state,
                    onSurfaceAvailable = onSurfaceAvailable,
                    onSurfaceRelease = onSurfaceRelease,
                )
                Header(
                    onBuyTangemClick = state.onBuyTangemClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                ) {
                    WelcomeTextScenes(
                        scenes = state.scenes,
                        isAnimated = isAnimated,
                        isVideoRunning = state.isVideoRunning,
                        videoPositionMs = videoPositionMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    )
                    BottomActions(state = state)
                }
            }
            // One sheet is composed from the first frame, with or without a config of its own: a hidden modal
            // shows nothing, and being composed is what lets it warm the machinery both sheets then reuse.
            val isSheetShown = state.bottomSheetConfig != null || state.haveWalletBottomSheetConfig != null
            CreateWalletBottomSheet(
                config = state.bottomSheetConfig,
                prewarm = rememberSheetPrewarmReady(isScreenCovered = !isAnimated || !state.isVideoReady) &&
                    !rememberEverShown(isShown = isSheetShown),
                onCollapseEnd = state.onCreateWalletSheetCollapsed,
            )
            state.haveWalletBottomSheetConfig?.let { HaveWalletBottomSheet(config = it) }
        }
    }
}

@Composable
private fun VideoBackground(
    state: IntroductionUM,
    onSurfaceAvailable: (SurfaceView) -> Unit,
    onSurfaceRelease: (SurfaceView) -> Unit,
) {
    val videoSize = state.videoSize
    // A SurfaceView punches a hole through the window, erasing whatever is composed before it, so the shutter
    // below has to come after it.
    key(state.videoSurfaceKey) {
        AndroidView(
            modifier = Modifier.videoSurfaceSize(aspectRatio = videoSize.width.toFloat() / videoSize.height),
            factory = { context ->
                SurfaceView(context).also { surfaceView ->
                    // The buffers keep the video's own size whatever the layout does, so no layout pass resizes
                    // the surface under the decoder.
                    surfaceView.holder.setFixedSize(videoSize.width, videoSize.height)
                    onSurfaceAvailable(surfaceView)
                }
            },
            onRelease = onSurfaceRelease,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (state.isVideoReady) 0f else 1f }
            .background(TangemTheme.colors3.bg.base),
    ) {
        // The video's own first frame, framed the way the surface frames it: the cut to playback lands on the
        // same picture, and a surface with nothing in it yet shows this instead of the hole it punches through
        // the window.
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = IntroductionR.drawable.img_introduction_background_poster),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rememberBottomScrimBrush()),
    )
}

/**
 * Whether the sheet of this screen may take its warm-up pass yet (the `prewarm` flag of `TangemModal`).
 *
 * The pass is a full composition and measurement of a modal card, and the first one in a process is the expensive
 * one — the few hundred milliseconds that otherwise sit between the viewer's first tap and the sheet starting to
 * move. It costs a frame, so it waits for one nobody sees: this is the cold-start screen, and until the video has
 * rendered its first frame (or is never going to) the shutter covers everything the pass could stutter.
 *
 * [PREWARM_COVER_TIMEOUT] bounds that wait — a screen that uncovers before the pass is taken still has to take it,
 * since one stuttered frame is worth far less than half a second on the tap.
 */
@Composable
private fun rememberSheetPrewarmReady(isScreenCovered: Boolean): Boolean {
    var isReady by remember { mutableStateOf(false) }
    val isCovered by rememberUpdatedState(isScreenCovered)
    LaunchedEffect(Unit) {
        // The screen's own first frame comes first: the pass must not be what delays it.
        withFrameNanos {}
        withTimeoutOrNull(PREWARM_COVER_TIMEOUT) { snapshotFlow { isCovered }.first { it } }
        isReady = true
    }
    return isReady
}

/** Latches once the viewer has opened a sheet: from then on the machinery is warm and the pass has nothing to give. */
@Composable
private fun rememberEverShown(isShown: Boolean): Boolean {
    var wasShown by remember { mutableStateOf(false) }
    if (isShown) wasShown = true
    return wasShown
}

@Composable
private fun rememberBottomScrimBrush(): Brush {
    val scrimColor = TangemTheme.colors3.bg.primary
    return remember(scrimColor) {
        val colorStops = ScrimGradient
            .map { (stop, alpha) -> stop to scrimColor.copy(alpha = alpha) }
            .toTypedArray()
        Brush.verticalGradient(colorStops = colorStops)
    }
}

@Composable
private fun Header(onBuyTangemClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier.height(20.dp),
            painter = painterResource(id = CoreUiR.drawable.ic_tangem_logo),
            contentScale = ContentScale.FillHeight,
            contentDescription = null,
        )
        TangemButton(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(weight = 1f, fill = false)
                // Nothing on this screen is a haze source, and the video is a SurfaceView no blur can sample, so the
                // material draws no fill at all. The tint iOS lays over its blur stands in for it.
                .background(color = TangemTheme.colors3.material.fill.blur, shape = CircleShape),
            text = resourceReference(R.string.introduction_button_buy_tangem),
            variant = TangemButton.Variant.Material,
            size = TangemButton.Size.X11,
            onClick = onBuyTangemClick,
        )
    }
}

@Composable
private fun BottomActions(state: IntroductionUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(R.string.introduction_button_create_wallet),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            onClick = state.onCreateWalletClick,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(R.string.introduction_button_i_have_a_wallet),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            onClick = state.onIHaveWalletClick,
        )
        LegalCaption(
            modifier = Modifier.padding(top = 16.dp),
            onTermsOfServiceClick = state.onTermsOfServiceClick,
            onPrivacyPolicyClick = null,
        )
    }
}

/**
 * Lays the surface out with the frame's own shape, large enough to cover the screen and centred on it.
 *
 * The crop the player asks the decoder for is a request: vendor decoders are free to ignore it and stretch the
 * frame into whatever shape the surface has, which on a screen taller than the video is what squeezes the
 * picture. A surface that already has the frame's shape renders the same either way.
 */
private fun Modifier.videoSurfaceSize(aspectRatio: Float): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val isCoveredByHeight = width < height * aspectRatio
    val coverWidth = if (isCoveredByHeight) (height * aspectRatio).roundToInt() else width
    val coverHeight = if (isCoveredByHeight) height else (width / aspectRatio).roundToInt()
    val placeable = measurable.measure(Constraints.fixed(width = coverWidth, height = coverHeight))
    layout(width = width, height = height) {
        placeable.place(x = (width - coverWidth) / 2, y = (height - coverHeight) / 2)
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun IntroductionScreenPreview() {
    TangemThemePreview {
        IntroductionScreen(
            onSurfaceAvailable = {},
            onSurfaceRelease = {},
            videoPositionMs = { 0L },
            state = IntroductionUM(
                isVideoReady = true,
                videoSize = IntSize(width = 1080, height = 1920),
                videoSurfaceKey = 0,
                isVideoFailed = false,
                isVideoRunning = false,
                isMotionEnabled = false,
                scenes = welcomeScenes,
                bottomSheetConfig = null,
                haveWalletBottomSheetConfig = null,
                onCreateWalletClick = {},
                onIHaveWalletClick = {},
                onBuyTangemClick = {},
                onCreateWalletSheetCollapsed = {},
                onTermsOfServiceClick = {},
            ),
        )
    }
}
// endregion Preview