package com.tangem.core.ui.ds2.scorescale

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.animation.TangemAnimationSpec
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 score scale: a round button per score from 0 to 10, laid out in centered rows.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=30565-154053)
 *
 * Behavior notes:
 * - Two rows, 0–5 and 6–10. As the width narrows, the gap first shrinks from 12 dp to 4 dp, then the scale
 *   takes more even rows (4-4-3, 3-3-3-2, …) instead of being clipped.
 * - The selected score is filled with the brand color, the others look like a secondary button.
 * - The buttons keep their 48 dp size: the digits follow the user's font scale up to 1.3×.
 *
 * Accessibility: the scale is a selectable group, and each score is announced as a radio button with
 * its digit, its selection state and, when [isEnabled] is `false`, as disabled — even when
 * [onScoreClick] is `null`.
 *
 * @param selectedScore The highlighted score; `null` while nothing is selected.
 * @param onScoreClick Invoked with the tapped score. `null` makes the scale non-interactive.
 * @param modifier Modifier applied to the scale; it fills the available width to center its rows.
 * @param isEnabled When `false`, the scale is dimmed and taps are ignored.
 */
@Composable
fun TangemScoreScale(
    selectedScore: Int?,
    onScoreClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Layout(
        modifier = modifier.selectableGroup(),
        content = {
            SCORES.forEach { score ->
                ScoreButton(
                    score = score,
                    isSelected = score == selectedScore,
                    onClick = onScoreClick?.let { { it(score) } },
                    isEnabled = isEnabled,
                )
            }
        },
    ) { measurables, constraints ->
        val buttonSize = BUTTON_SIZE.roundToPx()
        val minGap = MIN_GAP.roundToPx()
        val preferredGap = PREFERRED_GAP.roundToPx()
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            MAX_BUTTONS_IN_ROW * buttonSize + (MAX_BUTTONS_IN_ROW - 1) * preferredGap
        }
        val maxButtonsInRow = ((width + minGap) / (buttonSize + minGap)).coerceIn(1, MAX_BUTTONS_IN_ROW)
        val rowCount = (measurables.size + maxButtonsInRow - 1) / maxButtonsInRow
        val buttonsInRow = (measurables.size + rowCount - 1) / rowCount
        val gap = if (buttonsInRow > 1) {
            ((width - buttonsInRow * buttonSize) / (buttonsInRow - 1)).coerceIn(minGap, preferredGap)
        } else {
            0
        }
        val rowGap = ROW_GAP.roundToPx()
        val rows = measurables.map { it.measure(Constraints.fixed(buttonSize, buttonSize)) }.chunked(buttonsInRow)

        layout(width = width, height = rows.size * buttonSize + (rows.size - 1) * rowGap) {
            rows.forEachIndexed { rowIndex, row ->
                var x = (width - (row.size * buttonSize + (row.size - 1) * gap)) / 2
                row.forEach { button ->
                    button.place(x = x, y = rowIndex * (buttonSize + rowGap))
                    x += buttonSize + gap
                }
            }
        }
    }
}

/** Public API surface of [TangemScoreScale]. */
object TangemScoreScale {

    val version = DsVersion(major = 1, minor = 0, patch = 0)
}

@Suppress("MagicNumber")
@Composable
private fun ScoreButton(score: Int, isSelected: Boolean, onClick: (() -> Unit)?, isEnabled: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val pressColor by animateColorAsState(
        targetValue = if (isPressed) TangemTheme.colors3.interaction.press.default else Color.Transparent,
        animationSpec = TangemAnimationSpec.PressOverlay,
        label = "pressColor",
    )
    val colors = TangemTheme.colors3

    Box(
        modifier = Modifier
            .graphicsLayer { alpha = if (isEnabled) 1f else 0.4f }
            .semantics(mergeDescendants = true) {
                if (!isEnabled) disabled()
                if (onClick == null) {
                    role = Role.RadioButton
                    selected = isSelected
                }
            }
            .conditionalCompose(onClick != null) {
                selectable(
                    selected = isSelected,
                    enabled = isEnabled,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = requireNotNull(onClick),
                )
            }
            .clip(CircleShape)
            .background(if (isSelected) colors.bg.brand else colors.bg.transparent.primary)
            .drawBehind { drawRect(pressColor) }
            .conditionalCompose(isFocused) {
                val focusRing = colors.interaction.focusRing
                border(
                    width = 2.dp,
                    color = if (isSelected) focusRing.default else focusRing.brand,
                    shape = CircleShape,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalDensity provides cappedFontScaleDensity()) {
            Text(
                text = score.toString(),
                color = if (isSelected) colors.text.staticDark.primary else colors.text.primary,
                style = TangemTheme.typography3.body.medium,
            )
        }
    }
}

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

private val SCORES = 0..10
private const val MAX_BUTTONS_IN_ROW = 6
private const val MAX_FONT_SCALE = 1.3f
private val BUTTON_SIZE = 48.dp
private val PREFERRED_GAP = 12.dp
private val MIN_GAP = 4.dp
private val ROW_GAP = 16.dp

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemScoreScalePreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TangemScoreScale(selectedScore = null, onScoreClick = {})
            TangemScoreScale(selectedScore = 7, onScoreClick = {})
            TangemScoreScale(selectedScore = 7, onScoreClick = {}, isEnabled = false)
            TangemScoreScale(selectedScore = 0, onScoreClick = {}, modifier = Modifier.width(240.dp))
        }
    }
}