@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dot_12_filled
import com.tangem.core.ui.res.generated.icons.ic_triangle_down_12
import com.tangem.core.ui.res.generated.icons.ic_triangle_up_12

/**
 * Design-system v2 (DS3) **Util / Price Change** — a compact directional indicator: an up/down
 * triangle (or a dot for [TangemPriceChange.Direction.Neutral]) and a percent label colored by
 * [direction]. A "util" building block, not a complete component — embed it next to quotes and
 * balances (e.g. inside the token row).
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5724-3922&m=dev)
 *
 * @param value Formatted percent text (e.g. `"2.08%"`).
 * @param direction Change direction driving the arrow and its color. See
 * [TangemPriceChange.Direction].
 * @param modifier Modifier applied to the indicator root.
 * @param isFlickering Runs the blade animation over the label while the value is being refreshed.
 */
@Composable
fun TangemPriceChange(
    value: TextReference,
    direction: TangemPriceChange.Direction,
    modifier: Modifier = Modifier,
    isFlickering: Boolean = false,
) {
    val textColor = when (direction) {
        TangemPriceChange.Direction.Up -> TangemTheme.colors3.text.accent.blue
        TangemPriceChange.Direction.Down -> TangemTheme.colors3.text.accent.red
        TangemPriceChange.Direction.Neutral -> TangemTheme.colors3.text.tertiary
    }
    val iconTint = when (direction) {
        TangemPriceChange.Direction.Up -> TangemTheme.colors3.icon.accent.blue
        TangemPriceChange.Direction.Down -> TangemTheme.colors3.icon.accent.red
        TangemPriceChange.Direction.Neutral -> TangemTheme.colors3.icon.tertiary
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val arrow = when (direction) {
            TangemPriceChange.Direction.Up -> Icons.ic_triangle_up_12
            TangemPriceChange.Direction.Down -> Icons.ic_triangle_down_12
            TangemPriceChange.Direction.Neutral -> Icons.ic_dot_12_filled
        }
        Icon(
            imageVector = arrow,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = value.resolveReference(),
            style = TangemTheme.typography3.caption.medium.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = textColor,
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Design-system v2 (DS3) **Util / Price Change** — state-driven overload of [TangemPriceChange].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5724-3922&m=dev)
 *
 * @param state Indicator state model. See [TangemPriceChange.State].
 * @param modifier Modifier applied to the indicator root.
 * @param isFlickering Runs the blade animation over the label while the value is being refreshed.
 */
@Composable
@NonRestartableComposable
fun TangemPriceChange(state: TangemPriceChange.State, modifier: Modifier = Modifier, isFlickering: Boolean = false) {
    TangemPriceChange(
        value = state.value,
        direction = state.direction,
        modifier = modifier,
        isFlickering = isFlickering,
    )
}

/** Public API surface of [TangemPriceChange]. */
object TangemPriceChange {

    /** Direction of the price change, driving the leading icon and its color. */
    enum class Direction {
        /** Price went up — upward arrow, accent color. */
        Up,

        /** Price went down — downward arrow, error color. */
        Down,

        /** No significant change — dot icon, tertiary color. */
        Neutral,
    }

    /**
     * Price change state model.
     *
     * @param value Formatted percent text (e.g. `"2.08%"`).
     * @param direction Change direction. See [Direction].
     */
    @Immutable
    data class State(
        val value: TextReference,
        val direction: Direction,
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPriceChangePreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemPriceChange.Direction.entries.forEach { direction ->
                TangemPriceChange(
                    value = stringReference("2.08%"),
                    direction = direction,
                )
            }
            TangemPriceChange(
                state = TangemPriceChange.State(
                    value = stringReference("2.08%"),
                    direction = TangemPriceChange.Direction.Up,
                ),
                isFlickering = true,
            )
        }
    }
}