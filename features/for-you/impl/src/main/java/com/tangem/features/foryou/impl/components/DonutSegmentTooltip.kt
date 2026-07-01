package com.tangem.features.foryou.impl.components

import android.content.res.Configuration
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Frosted-glass selection tooltip for a [DonutChart] slice.
 *
 * Modeled on [com.tangem.core.ui.ds.contextmenu.TangemContextMenu]: it shows a [Popup] with the same
 * springy pop-in animation, but renders the content on a translucent [TangemSurface] (`isMaterial = true`)
 * for the glass-morphism look instead of the context menu's opaque card. The pill is centered over the
 * chart's anchor (see [CenteredOverAnchorPositionProvider]).
 *
 * The popup is intentionally **non-focusable**, so its window is not modal: taps outside the pill pass
 * straight through to the [DonutChart] underneath, letting the user switch to another slice in a single
 * tap. `dismissOnClickOutside` stays on, so a tap anywhere off the pill — including outside the chart card,
 * elsewhere on the host screen — fires [onDismissRequest].
 *
 * Caveat the caller must handle: that outside-tap detection fires on the **DOWN**, before the chart's tap
 * resolves on the UP, and it also fires for taps that land on the chart. So [onDismissRequest] alone can't
 * tell "tapped the chart" from "tapped elsewhere" — the caller must veto/defer it when the press actually
 * landed on the chart (see `MarketChart`), otherwise selecting a slice would briefly clear the selection
 * first and flicker the popup.
 *
 * @param expanded Whether the tooltip is shown. Toggling to `false` plays the scale-out before the popup
 *   leaves the composition.
 * @param title Asset name shown on the first line (e.g. `"Ethereum"`).
 * @param fiatValue Pre-formatted fiat value shown on the second line (e.g. `"$5,720.22"`).
 * @param percent Pre-formatted share shown after the value, dimmed (e.g. `"57.5%"`).
 * @param onDismissRequest Fired on a tap anywhere off the pill (DOWN). Clear the selection here, but see the
 *   caveat above: defer it so a press on the chart can veto it before it commits.
 * @param modifier Modifier applied to the pill surface.
 * @param positionProvider Where the pill is placed. Defaults to centered over the anchor; pass a
 *   [SegmentTooltipPositionProvider] to anchor it to the end of the selected slice.
 */
@Suppress("LongParameterList")
@Composable
internal fun DonutSegmentTooltip(
    expanded: Boolean,
    title: String,
    fiatValue: String,
    percent: String,
    positionProvider: PopupPositionProvider,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = positionProvider,
            properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
        ) {
            TooltipPill(
                expandedStates = expandedStates,
                title = title,
                fiatValue = fiatValue,
                percent = percent,
                modifier = modifier,
            )
        }
    }
}

private const val OUT_TRANSITION_DURATION = 75
private const val ENTER_SPRING_DAMPING = 0.82f
private const val ENTER_SPRING_STIFFNESS = 1100f
private const val DISMISSED_SCALE = 0.8f

@Suppress("MagicNumber")
@Composable
private fun TooltipPill(
    expandedStates: MutableTransitionState<Boolean>,
    title: String,
    fiatValue: String,
    percent: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberTransition(expandedStates, label = "DonutSegmentTooltip")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                spring(dampingRatio = ENTER_SPRING_DAMPING, stiffness = ENTER_SPRING_STIFFNESS)
            } else {
                tween(durationMillis = 1, delayMillis = OUT_TRANSITION_DURATION - 1)
            }
        },
        label = "scale",
    ) { isExpanded -> if (isExpanded) 1f else DISMISSED_SCALE }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                tween(durationMillis = 30)
            } else {
                tween(durationMillis = OUT_TRANSITION_DURATION)
            }
        },
        label = "alpha",
    ) { isExpanded -> if (isExpanded) 1f else 0f }

    TangemSurface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            transformOrigin = TransformOrigin.Center
        },
        isMaterial = true,
        shape = RoundedCornerShape(percent = 50),
        shadowRadius = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fiatValue,
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                )
                Text(
                    text = "  •  $percent%",
                    color = TangemTheme.colors3.text.tertiary,
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                )
            }
        }
    }
}

// region Preview

@Suppress("MagicNumber")
@Preview(name = "DonutSegmentTooltip • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "DonutSegmentTooltip • Light", showBackground = true)
@Composable
private fun PreviewDonutSegmentTooltip() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            TooltipPill(
                expandedStates = remember { MutableTransitionState(true) },
                title = "Ethereum",
                fiatValue = "$5,720.22",
                percent = "57.5",
            )
        }
    }
}

// endregion