package com.tangem.features.polymarket.impl.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * A market outcome button: the price is always shown whole, the title gives way and truncates with an ellipsis.
 *
 * The design-system button auto-sizes its single label, so a long outcome shrinks the text and cuts the price
 * together with the title. This button never scales its text and repeats the design-system secondary info /
 * error appearance on the shared surface. It hugs its content unless the caller constrains the width.
 *
 * @param title outcome label as served by the backend
 * @param appearance accent of the button, see [PolymarketOutcomeButton.Appearance]
 * @param size height preset, see [PolymarketOutcomeButton.Size]
 * @param price share price shown after the title, e.g. "25¢"; `null` shows the title alone
 */
@Composable
internal fun PolymarketOutcomeButton(
    title: TextReference,
    appearance: PolymarketOutcomeButton.Appearance,
    size: PolymarketOutcomeButton.Size,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    price: TextReference? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val colors = appearance.colors()
    val dimensions = size.dimensions()

    TangemSurface(
        modifier = modifier.semantics(mergeDescendants = true) { role = Role.Button },
        color = colors.background,
        border = if (isFocused) {
            BorderStroke(width = FOCUS_RING_WIDTH, color = TangemTheme.colors3.interaction.focusRing.brand)
        } else {
            null
        },
        shape = CircleShape,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(LocalDensity provides cappedFontScaleDensity()) {
            Row(
                modifier = Modifier
                    .heightIn(min = dimensions.height)
                    .widthIn(min = dimensions.minWidth)
                    .padding(horizontal = dimensions.horizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = title.resolveReference(),
                    color = colors.content,
                    style = labelStyle(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                if (price != null) {
                    Text(
                        text = PRICE_SEPARATOR + price.resolveReference(),
                        color = colors.content,
                        style = labelStyle(),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

internal object PolymarketOutcomeButton {

    /** Accent of the button: the first outcome of a market is [Info], the second is [Error]. */
    enum class Appearance {
        Info,
        Error,
    }

    /** Height preset, named after the design-system button size it matches. */
    enum class Size {
        X9,
        X11,
    }
}

private class AppearanceColors(val background: Color, val content: Color)

private class SizeDimensions(val height: Dp, val minWidth: Dp, val horizontalPadding: Dp)

@Composable
@ReadOnlyComposable
private fun PolymarketOutcomeButton.Appearance.colors(): AppearanceColors = when (this) {
    PolymarketOutcomeButton.Appearance.Info -> AppearanceColors(
        background = TangemTheme.colors3.bg.status.infoSubtle,
        content = TangemTheme.colors3.text.accent.blue,
    )
    PolymarketOutcomeButton.Appearance.Error -> AppearanceColors(
        background = TangemTheme.colors3.bg.status.errorSubtle,
        content = TangemTheme.colors3.text.accent.red,
    )
}

private fun PolymarketOutcomeButton.Size.dimensions(): SizeDimensions = when (this) {
    PolymarketOutcomeButton.Size.X9 -> SizeDimensions(height = 36.dp, minWidth = 56.dp, horizontalPadding = 14.dp)
    PolymarketOutcomeButton.Size.X11 -> SizeDimensions(height = 44.dp, minWidth = 72.dp, horizontalPadding = 18.dp)
}

@Composable
@ReadOnlyComposable
private fun labelStyle(): TextStyle = TangemTheme.typography3.body.medium.copy(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** The button height is fixed, so the user's font scale is honoured only up to [MAX_FONT_SCALE]. */
@Composable
private fun cappedFontScaleDensity(): Density {
    val density = LocalDensity.current
    return remember(density.density, density.fontScale) {
        if (density.fontScale <= MAX_FONT_SCALE) {
            density
        } else {
            Density(density = density.density, fontScale = MAX_FONT_SCALE)
        }
    }
}

private const val PRICE_SEPARATOR = " • "
private const val MAX_FONT_SCALE = 1.3f
private val FOCUS_RING_WIDTH = 2.dp

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOutcomeButtonPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PolymarketOutcomeButton(
                    title = stringReference("Yes"),
                    appearance = PolymarketOutcomeButton.Appearance.Info,
                    size = PolymarketOutcomeButton.Size.X9,
                    onClick = {},
                )
                PolymarketOutcomeButton(
                    modifier = Modifier.widthIn(max = 88.dp),
                    title = stringReference("Neymar Jr. da Silva Santos"),
                    appearance = PolymarketOutcomeButton.Appearance.Error,
                    size = PolymarketOutcomeButton.Size.X9,
                    onClick = {},
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PolymarketOutcomeButton(
                    modifier = Modifier.weight(1f),
                    title = stringReference("Yes"),
                    price = stringReference("25¢"),
                    appearance = PolymarketOutcomeButton.Appearance.Info,
                    size = PolymarketOutcomeButton.Size.X11,
                    onClick = {},
                )
                PolymarketOutcomeButton(
                    modifier = Modifier.weight(1f),
                    title = stringReference("Elon Musk becomes president"),
                    price = stringReference("74¢"),
                    appearance = PolymarketOutcomeButton.Appearance.Error,
                    size = PolymarketOutcomeButton.Size.X11,
                    onClick = {},
                )
            }
        }
    }
}