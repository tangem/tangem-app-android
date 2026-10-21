package com.tangem.features.introduction.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import com.tangem.core.media.LoopingVideoState
import com.tangem.core.media.VideoFrameSize
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.media.ui.VideoSurface
import com.tangem.core.res.R
import com.tangem.core.ui.components.KeepScreenOn
import com.tangem.core.ui.components.legal.LegalCaption
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.introduction.impl.model.welcomeScenes
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.features.introduction.impl.R as IntroductionR
import com.tangem.core.ui.R as CoreUiR

private const val PROGRESS_FADE_MS = 200

/** Vertical stops of the darkening gradient, each paired with the scrim opacity it reaches. */
private val ScrimGradient = listOf(
    0.52f to 0f,
    0.63f to 0.5f,
    0.70f to 0.8f,
    0.76f to 0.96f,
    0.82f to 1f,
    1f to 1f,
)

/** The social login overlays consume taps, but accessibility focus would walk straight past them */
private val HiddenFromAccessibility = Modifier.clearAndSetSemantics {}

/** How long the warm-up pass waits for a frame nobody sees before taking one that someone might. */
private val PREWARM_COVER_TIMEOUT = 1.seconds

@Composable
internal fun IntroductionScreen(
    state: IntroductionUM,
    video: LoopingVideoState,
    videoPositionMs: () -> Long,
    modifier: Modifier = Modifier,
) {
    SystemBarsIconsDisposable(darkIcons = false)
    val isAnimated = video.isMotionEnabled && !video.isFailed
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
                VideoBackground(surface = video.surface)
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
                        .padding(bottom = 12.dp)
                        .then(if (state.isSocialLoginShown) HiddenFromAccessibility else Modifier),
                ) {
                    WelcomeTextScenes(
                        scenes = state.scenes,
                        isAnimated = isAnimated,
                        isVideoRunning = video.isRunning,
                        videoPositionMs = videoPositionMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    )
                    BottomActions(state = state)
                }
                SocialLoginProgressHost(progress = state.socialLoginProgress)
                state.socialLoginConsent?.let { SocialLoginConsentScreen(state = it) }
            }
            // One sheet is composed from the first frame, with or without a config of its own: a hidden modal
            // shows nothing, and being composed is what lets it warm the machinery both sheets then reuse.
            val isSheetShown = state.bottomSheetConfig != null || state.haveWalletBottomSheetConfig != null
            val isScreenCovered = !isAnimated || !video.surface.isFirstFrameShown
            CreateWalletBottomSheet(
                config = state.bottomSheetConfig,
                prewarm = rememberSheetPrewarmReady(isScreenCovered = isScreenCovered) &&
                    !rememberEverShown(isShown = isSheetShown),
                onCollapseEnd = state.onCreateWalletSheetCollapsed,
            )
            state.haveWalletBottomSheetConfig?.let { HaveWalletBottomSheet(config = it) }
        }
    }
}

@Composable
private fun VideoBackground(surface: VideoSurfaceState) {
    VideoSurface(state = surface, modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.base),
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
 * rendered its first frame (or is never going to) the poster covers everything the pass could stutter.
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
private fun SocialLoginProgressHost(progress: SocialLoginProgressUM?) {
    AnimatedContent(
        targetState = progress,
        modifier = Modifier.fillMaxSize(),
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
            onPrivacyPolicyClick = state.onPrivacyPolicyClick,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun IntroductionScreenPreview() {
    TangemThemePreview {
        IntroductionScreen(
            videoPositionMs = { 0L },
            video = LoopingVideoState(
                surface = VideoSurfaceState(
                    frameSize = VideoFrameSize(width = 1080, height = 1920),
                    key = 0,
                    isFirstFrameShown = true,
                    onAvailable = {},
                    onRelease = {},
                ),
                isRunning = false,
                isMotionEnabled = false,
                isFailed = false,
            ),
            state = IntroductionUM(
                scenes = welcomeScenes,
                bottomSheetConfig = null,
                haveWalletBottomSheetConfig = null,
                socialLoginProgress = null,
                socialLoginConsent = null,
                onCreateWalletClick = {},
                onIHaveWalletClick = {},
                onBuyTangemClick = {},
                onCreateWalletSheetCollapsed = {},
                onTermsOfServiceClick = {},
                onPrivacyPolicyClick = {},
            ),
        )
    }
}
// endregion Preview