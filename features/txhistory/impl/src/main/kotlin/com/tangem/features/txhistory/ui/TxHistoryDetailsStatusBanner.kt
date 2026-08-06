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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_success_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.StatusBannerUM
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.StatusBannerUM.Style
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

/** Key for the title [AnimatedContent]: the resolved [text] plus the [style] that selects the swap motion. */
private data class StatusBannerTitle(val text: String, val style: Style)

/**
 * Title transition picked by the *target* style: the in-progress/success looks slide in from the right ([titleSlide]);
 * the alerting terminals float up from below ([titleRise]). Both fade the old status out fully before fading the new in.
 */
private fun titleTransition(target: Style): ContentTransform = when (target) {
    Style.Warning, Style.Error, Style.Refunded, Style.Expired -> titleRise()
    Style.Info, Style.Success -> titleSlide()
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

    // Auto-hide rules for the success terminal ("Confirmed"). It is the only [Style.Success] state and must read as a
    // *transition*, not a resting state: opening the details on an already-finished deal (no in-flight status was ever
    // seen) shows nothing, and once it does appear it lingers only briefly before collapsing. Failure / verification
    // terminals are not Success, so they stay put.
    val seenNonSuccess = remember { mutableStateOf(false) }
    SideEffect { if (state != null && state.style != Style.Success) seenNonSuccess.value = true }

    val isTerminalSuccess = state?.style == Style.Success
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
        targetValue = state.style.backgroundColor(),
        // Delayed into Phase 2, so the tint starts shifting only once the old title has faded out, matching the spec.
        animationSpec = tween(durationMillis = DEFAULT_ANIMATION_MILLIS, delayMillis = ENTER_DELAY_MILLIS),
        label = "StatusBannerBackground",
    )
    val contentColor = state.style.contentColor()

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
            // Animate the title as the status advances. Keyed on (text, style) so [titleTransition] picks the motion
            // by target; the key also colors each content from its own style (see [color] below).
            AnimatedContent(
                targetState = StatusBannerTitle(state.title.resolveReference(), state.style),
                transitionSpec = { titleTransition(target = targetState.style) },
                label = "StatusBannerTitle",
                modifier = Modifier.weight(1f),
            ) { title ->
                Text(
                    text = title.text,
                    style = TangemTheme.typography3.body.medium,
                    // From this title's own key, so the outgoing title fades out in its colour instead of snapping.
                    color = title.style.contentColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusBannerTrailing(isLoading = state.isLoading, style = state.style)
        }
        // Retain the last non-null subtitle so the line stays rendered while it fades out (mirrors the retain above).
        val lastSubtitle = remember { mutableStateOf<TextReference?>(null) }
        SideEffect {
            state.subtitle?.let { subtitle -> lastSubtitle.value = subtitle }
        }

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
            val subtitle = state.subtitle ?: lastSubtitle.value
            subtitle?.let { line ->
                Text(
                    text = line.resolveAnnotatedReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = contentColor,
                    modifier = Modifier.padding(top = SUBTITLE_TOP_GAP),
                )
            }
        }
    }
}

/**
 * Key for the trailing [AnimatedContent]: whether the loader or a glyph shows, and the [style] that tints and picks it.
 */
private data class StatusBannerGlyph(val isLoading: Boolean, val style: Style)

/** Trailing slot: rotating loader while in progress, the [style]'s static status glyph otherwise. */
@Composable
private fun StatusBannerTrailing(isLoading: Boolean, style: Style, modifier: Modifier = Modifier) {
    // Keyed on (isLoading, style) so the tint/glyph come from each content's own key — the outgoing loader then fades
    // out in its colour/glyph instead of snapping to the incoming status'.
    AnimatedContent(
        targetState = StatusBannerGlyph(isLoading, style),
        transitionSpec = { iconSwapTransition() },
        label = "StatusBannerTrailing",
        modifier = modifier,
    ) { glyph ->
        val tint = glyph.style.contentColor()
        if (glyph.isLoading) {
            TangemLoader(size = TangemLoaderSize.X20, color = tint)
        } else {
            Icon(
                imageVector = glyph.style.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun Style.backgroundColor(): Color = when (this) {
    Style.Info -> TangemTheme.colors3.bg.status.infoSubtle
    Style.Success -> TangemTheme.colors3.bg.status.successSubtle
    Style.Error, Style.Refunded -> TangemTheme.colors3.bg.status.errorSubtle
    Style.Warning -> TangemTheme.colors3.bg.status.warningSubtle
    Style.Expired -> TangemTheme.colors3.bg.tertiary
}

@Composable
private fun Style.contentColor(): Color = when (this) {
    Style.Info -> TangemTheme.colors3.text.status.info
    Style.Success -> TangemTheme.colors3.text.status.success
    Style.Error, Style.Refunded -> TangemTheme.colors3.text.status.error
    Style.Warning -> TangemTheme.colors3.text.status.warning
    Style.Expired -> TangemTheme.colors3.text.tertiary
}

private fun Style.icon() = when (this) {
    Style.Success -> Icons.ic_success_20
    Style.Error -> Icons.ic_error_20
    Style.Warning -> Icons.ic_warning_20
    Style.Info -> Icons.ic_info_20
    Style.Refunded -> Icons.ic_arrow_refresh_20
    Style.Expired -> Icons.ic_clock_20
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
                state = StatusBannerUM(Style.Info, stringReference("Awaiting funds"), isLoading = true),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(Style.Info, stringReference("Deposit confirmed"), isLoading = true),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(Style.Success, stringReference("Confirmed"), isLoading = false),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(
                    style = Style.Error,
                    title = stringReference("Failed"),
                    subtitle = stringReference("Visit provider's website to refund your money"),
                    isLoading = false,
                ),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(
                    style = Style.Warning,
                    title = stringReference("Verification required"),
                    subtitle = stringReference("Visit provider's website to refund your money"),
                    isLoading = false,
                ),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(Style.Expired, stringReference("Expired"), isLoading = false),
            )
            TxHistoryDetailsStatusBanner(
                state = StatusBannerUM(
                    style = Style.Refunded,
                    title = stringReference("Refunded in WBTC"),
                    subtitle = stringReference(
                        "Your funds have been refunded in WBTC to your wallet on the Polygon network, " +
                            "in accordance with OKX exchange rules. ",
                    ) + styledStringReference(
                        value = "Learn more",
                        spanStyleReference = { SpanStyle(textDecoration = TextDecoration.Underline) },
                        onClick = {},
                    ),
                    isLoading = false,
                ),
            )
        }
    }
}
// endregion