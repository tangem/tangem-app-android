package com.tangem.features.foryou.impl.components.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import java.math.BigDecimal

/**
 * One colored slice of a [com.tangem.features.foryou.impl.components.DonutChart].
 *
 * @param weight Fraction of the full circle this slice occupies, in `0f..1f`. The slices are laid out
 *   contiguously; whatever is left after `sum(weight)` shows through as the track.
 *   For a portfolio where slices sum to 1f the ring fills completely and no track is visible.
 *   Also doubles as the slice's share for the selection tooltip (rendered as `weight * 100%`).
 * @param color Palette entry for the slice's solid fill. Assigned by the producer in segment order, so the
 *   slice's colour follows its rank; [DonutChart] resolves it to a themed [Color] in composition.
 * @param title Human-readable name of the asset this slice represents (e.g. `"Ethereum"`). Shown in the
 *   selection tooltip. Empty by default for slices that don't need a label.
 * @param fiatValue Pre-formatted fiat value of the slice (e.g. `"$5,720.22"`). Shown in the selection
 *   tooltip next to the share. Empty by default.
 */

@Immutable
internal data class DonutSegmentUM(
    val color: DonutSegmentColor,
    val weight: BigDecimal,
    val title: TextReference,
    val fiatValue: TextReference,
)

internal enum class DonutSegmentColor {
    Brand,
    Violet,
    Red,
    Green,
    ;

    @ReadOnlyComposable
    @Composable
    fun getColor(): Color {
        return when (this) {
            Brand -> TangemTheme.colors3.border.brand
            Violet -> TangemTheme.colors3.border.accent.violet
            Red -> TangemTheme.colors3.border.accent.red
            Green -> TangemTheme.colors3.border.accent.green
        }
    }
}