package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_success_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.StatusBannerUM
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.StatusBannerUM.Severity
import kotlinx.coroutines.delay

// Animation timings in ms (ProtoPie spec). The status swap is two-phase: the old status fades out, then the new one
// fades/slides in after ENTER_DELAY. Most steps run over the default duration; the trailing loader/glyph fades faster
// (FAST_FADE), and the plaque grows over GROW to make room for a subtitle.
private const val DEFAULT_ANIMATION_MILLIS = 300
private const val FAST_FADE_MILLIS = 200
private const val GROW_MILLIS = 400
private const val ENTER_DELAY_MILLIS = DEFAULT_ANIMATION_MILLIS // phase 2 waits for the phase-1 fade-out to clear
private const val SUBTITLE_DELAY_MILLIS = ENTER_DELAY_MILLIS + 100 // subtitle trails the title

/** How long the success terminal ("Confirmed") lingers before the plaque auto-collapses — it shows only as a transition. */
private const val CONFIRMED_VISIBLE_MILLIS = 1_000L

private const val TITLE_SLIDE_FRACTION = 12 // in-progress/Success title slides in 1/12 width from the right
private const val CONTENT_RISE_FRACTION = 2 // Warning/Error title floats up 1/2 height from below
private const val ICON_ENTER_SCALE = 0.6f

/** Gap between the exchange block above and the plaque; kept inside the collapsing region so it folds away cleanly. */
private val BANNER_TOP_GAP = 12.dp

/** Gap between the title row and the subtitle; lives inside the subtitle slot so it folds away when there's no line. */
private val SUBTITLE_TOP_GAP = 4.dp

/** Key for the title [AnimatedContent]: the resolved [text] plus the [severity] that selects the swap motion. */
private data class StatusBannerTitle(val text: String, val severity: Severity)

/**
 * Title transition picked by the *target* severity: Info/Success slide in from the right ([titleSlide]); Warning/Error
 * float up from below ([titleRise]). Both fade the old status out fully before fading the new one in.
 */
private fun titleTransition(target: Severity): ContentTransform = when (target) {
    Severity.Warning, Severity.Error -> titleRise()
    Severity.Info, Severity.Success -> titleSlide()
}

/** In-progress / success swap: old status fades out, new one fades in sliding from the right. */
private fun titleSlide(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS)) +
        slideInHorizontally(
            animationSpec = tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS),
        ) { width -> width / TITLE_SLIDE_FRACTION },
    initialContentExit = fadeOut(tween(durationMillis = DEFAULT_ANIMATION_MILLIS)),
    sizeTransform = SizeTransform(clip = false) { _, _ -> snap() },
)

/** Terminal warning / error swap: old status fades out, new one fades in floating up a touch from below. */
private fun titleRise(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS)) +
        slideInVertically(
            animationSpec = tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS),
        ) { height -> height / CONTENT_RISE_FRACTION },
    initialContentExit = fadeOut(tween(durationMillis = DEFAULT_ANIMATION_MILLIS)),
    sizeTransform = SizeTransform(clip = false) { _, _ -> snap() },
)

/** Trailing-slot swap (loader → glyph): loader fades out (Phase 1), then the glyph "pops" in (Phase 2). */
private fun iconSwapTransition(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(tween(durationMillis = FAST_FADE_MILLIS, delayMillis = ENTER_DELAY_MILLIS)) +
        scaleIn(
            animationSpec = tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS),
            initialScale = ICON_ENTER_SCALE,
        ),
    initialContentExit = fadeOut(tween(durationMillis = FAST_FADE_MILLIS)),
    sizeTransform = SizeTransform(clip = false) { _, _ -> snap() },
)

/**
 * Express status plaque of the Swap / Onramp transaction details, rendered under the two-asset exchange block.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=1370-114172)
 *
 * Two animation layers: [AnimatedVisibility] grows the plaque in from its top edge / collapses it to the bottom;
 * in-place status transitions ([StatusBannerContent]) morph the title, background tint and trailing loader→glyph as
 * the model re-emits the latest [state].
 *
 * @param state Current status to render, or `null` to hide the plaque (animated out).
 * @param modifier Modifier applied to the plaque container.
 */
@Composable
internal fun TxHistoryDetailsStatusBanner(state: StatusBannerUM?, modifier: Modifier = Modifier) {
    // Retain the last non-null state so content stays rendered through the exit (collapse+fade). The retained value
    // only backfills the exit (when [state] is null); published in a SideEffect, not written during composition.
    val lastState = remember { mutableStateOf<StatusBannerUM?>(null) }
    SideEffect { if (state != null) lastState.value = state }
    val content = state ?: lastState.value

    // Auto-hide rules for the success terminal ("Confirmed"). It is the only [Severity.Success] state and must read as a
    // *transition*, not a resting state: opening the details on an already-finished deal (no in-flight status was ever
    // seen) shows nothing, and once it does appear it lingers only briefly before collapsing. Failure / verification
    // terminals are not Success, so they stay put.
    val seenNonSuccess = remember { mutableStateOf(false) }
    SideEffect { if (state != null && state.severity != Severity.Success) seenNonSuccess.value = true }

    val isTerminalSuccess = state?.severity == Severity.Success
    val confirmedDismissed = remember { mutableStateOf(false) }
    LaunchedEffect(isTerminalSuccess) {
        if (isTerminalSuccess && seenNonSuccess.value) {
            delay(CONFIRMED_VISIBLE_MILLIS)
            confirmedDismissed.value = true
        }
    }

    val isVisible = when {
        state == null -> false
        isTerminalSuccess && !seenNonSuccess.value -> false // opened already on the success terminal → never shown
        isTerminalSuccess && confirmedDismissed.value -> false // "Confirmed" lingered long enough → collapse away
        else -> true
    }

    AnimatedVisibility(
        visible = isVisible,
        // Fade and size share one tween so alpha and height finish together (mismatched default springs leave a jerk).
        enter = fadeIn(tween(DEFAULT_ANIMATION_MILLIS)) +
            expandVertically(tween(DEFAULT_ANIMATION_MILLIS), expandFrom = Alignment.Top),
        exit = fadeOut(tween(DEFAULT_ANIMATION_MILLIS)) +
            shrinkVertically(tween(DEFAULT_ANIMATION_MILLIS), shrinkTowards = Alignment.Bottom),
        modifier = modifier,
    ) {
        // Leading gap lives inside the animated region so it collapses together with the plaque (no residual margin).
        content?.let { StatusBannerContent(state = it, modifier = Modifier.padding(top = BANNER_TOP_GAP)) }
    }
}

@Composable
private fun StatusBannerContent(state: StatusBannerUM, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(
        targetValue = state.severity.backgroundColor(),
        // Delayed into Phase 2, so the tint starts shifting only once the old title has faded out, matching the spec.
        animationSpec = tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS),
        label = "StatusBannerBackground",
    )
    val contentColor = state.severity.contentColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Animate the title as the status advances. Keyed on (text, severity) so [titleTransition] picks the motion
            // by target; the key also colors each content from its own severity (see [color] below).
            AnimatedContent(
                targetState = StatusBannerTitle(state.title.resolveReference(), state.severity),
                transitionSpec = { titleTransition(target = targetState.severity) },
                label = "StatusBannerTitle",
                modifier = Modifier.weight(1f),
            ) { title ->
                Text(
                    text = title.text,
                    style = TangemTheme.typography3.body.medium,
                    // From this title's own key, so the outgoing title fades out in its colour instead of snapping.
                    color = title.severity.contentColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusBannerTrailing(isLoading = state.isLoading, severity = state.severity)
        }
        // Retain the last non-null subtitle so the line stays rendered while it fades out (mirrors the retain above).
        val lastSubtitle = remember { mutableStateOf<TextReference?>(null) }
        SideEffect { if (state.subtitle != null) lastSubtitle.value = state.subtitle }

        AnimatedVisibility(
            visible = state.subtitle != null,
            // The subtitle owns the plaque's growth: expandVertically opens its slot in Phase 2, then the text fades in
            // a touch later so it trails the title. expandVertically (not animateContentSize) lets us delay the growth.
            enter = expandVertically(
                animationSpec = tween(GROW_MILLIS, delayMillis = ENTER_DELAY_MILLIS),
                expandFrom = Alignment.Top,
            ) + fadeIn(tween(DEFAULT_ANIMATION_MILLIS, delayMillis = SUBTITLE_DELAY_MILLIS)),
            exit = shrinkVertically(tween(DEFAULT_ANIMATION_MILLIS), shrinkTowards = Alignment.Top) +
                fadeOut(tween(DEFAULT_ANIMATION_MILLIS)),
        ) {
            (state.subtitle ?: lastSubtitle.value)?.let { subtitle ->
                Text(
                    text = subtitle.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = contentColor,
                    modifier = Modifier.padding(top = SUBTITLE_TOP_GAP),
                )
            }
        }
    }
}

/** Key for the trailing [AnimatedContent]: whether the loader or a glyph shows, plus the [severity] that tints it. */
private data class StatusBannerGlyph(val isLoading: Boolean, val severity: Severity)

/** Trailing slot: rotating loader while in progress, the static severity status glyph once terminal. */
@Composable
private fun StatusBannerTrailing(isLoading: Boolean, severity: Severity, modifier: Modifier = Modifier) {
    // Keyed on (isLoading, severity) so the tint comes from each content's own key — the outgoing loader then fades
    // out in its colour instead of snapping to the incoming status'.
    AnimatedContent(
        targetState = StatusBannerGlyph(isLoading, severity),
        transitionSpec = { iconSwapTransition() },
        label = "StatusBannerTrailing",
        modifier = modifier,
    ) { glyph ->
        val tint = glyph.severity.contentColor()
        if (glyph.isLoading) {
            TangemLoader(size = TangemLoaderSize.X20, color = tint)
        } else {
            Icon(
                imageVector = glyph.severity.statusIcon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun Severity.backgroundColor(): Color = when (this) {
    Severity.Info -> TangemTheme.colors3.bg.status.infoSubtle
    Severity.Success -> TangemTheme.colors3.bg.status.successSubtle
    Severity.Error -> TangemTheme.colors3.bg.status.errorSubtle
    Severity.Warning -> TangemTheme.colors3.bg.status.warningSubtle
}

@Composable
private fun Severity.contentColor(): Color = when (this) {
    Severity.Info -> TangemTheme.colors3.text.status.info
    Severity.Success -> TangemTheme.colors3.text.status.success
    Severity.Error -> TangemTheme.colors3.text.status.error
    Severity.Warning -> TangemTheme.colors3.text.status.warning
}

private fun Severity.statusIcon() = when (this) {
    Severity.Success -> Icons.ic_success_20
    Severity.Error -> Icons.ic_error_20
    Severity.Warning -> Icons.ic_warning_20
    Severity.Info -> Icons.ic_info_20
}

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TxHistoryDetailsStatusBannerPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(Severity.Info, stringReference("Awaiting funds"), isLoading = true),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(Severity.Info, stringReference("Deposit confirmed"), isLoading = true),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(Severity.Success, stringReference("Confirmed"), isLoading = false),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(
                    severity = Severity.Error,
                    title = stringReference("Failed"),
                    subtitle = stringReference("Visit provider's website to refund your money"),
                    isLoading = false,
                ),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(
                    severity = Severity.Warning,
                    title = stringReference("Verification required"),
                    subtitle = stringReference("Visit provider's website to refund your money"),
                    isLoading = false,
                ),
            )
        }
    }
}
// endregion