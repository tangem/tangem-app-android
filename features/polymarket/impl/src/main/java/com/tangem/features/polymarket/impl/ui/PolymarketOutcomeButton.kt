package com.tangem.features.polymarket.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Outcome (e.g. "Yes" / "No") pill button of a prediction-market card.
 *
 * A feature-local button: the design paints outcomes with the subtle info/error fills, which the
 * design-system button does not carry (Figma's Button has no such variants), so the pill is
 * assembled here from DS tokens instead of extending [com.tangem.core.ui.ds2.button.TangemButton].
 *
 * @param title button label — the outcome title as provided by the backend, optionally with the
 *  share price (e.g. "Yes • 25¢")
 * @param isPositive `true` for the affirmative side of the pair (info tint), `false` for the
 *  negative one (error tint). Driven by position, not by label, since upstream labels aren't
 *  always "Yes"/"No"
 * @param onClick opens the place-prediction flow for this outcome
 * @param modifier modifier applied to the button; pass a width modifier (e.g. `weight(1f)`) to
 *  stretch it, otherwise it hugs its label
 * @param minHeight minimum height of the pill
 */
@Composable
internal fun PolymarketOutcomeButton(
    title: TextReference,
    isPositive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 36.dp,
) {
    val background = if (isPositive) {
        TangemTheme.colors3.bg.status.infoSubtle
    } else {
        TangemTheme.colors3.bg.status.errorSubtle
    }
    val textColor = if (isPositive) {
        TangemTheme.colors3.text.accent.blue
    } else {
        TangemTheme.colors3.text.accent.red
    }
    TangemSurface(
        modifier = modifier,
        color = background,
        shape = CircleShape,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = minHeight)
                .defaultMinSize(minWidth = 64.dp)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.resolveReference(),
                color = textColor,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketOutcomeButtonPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PolymarketOutcomeButton(
                    title = stringReference("Yes"),
                    isPositive = true,
                    onClick = {},
                )
                PolymarketOutcomeButton(
                    title = stringReference("No"),
                    isPositive = false,
                    onClick = {},
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PolymarketOutcomeButton(
                    title = stringReference("Yes • 25¢"),
                    isPositive = true,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    minHeight = 44.dp,
                )
                PolymarketOutcomeButton(
                    title = stringReference("No • 74¢"),
                    isPositive = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    minHeight = 44.dp,
                )
            }
        }
    }
}