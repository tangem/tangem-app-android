package com.tangem.core.ui.ds2.button

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for [TangemButton]. Lets callers keep button content, style and click handlers inside their
 * own UI models.
 *
 * @param text Optional label. `null` switches the button to icon-only mode.
 * @param variant Visual style. See [TangemButton.Variant].
 * @param size Size preset. See [TangemButton.Size].
 * @param isLoading When `true`, hides the content and shows a loader.
 * @param isEnabled When `false`, the button is dimmed and clicks are ignored.
 * @param isDimmed When `true`, the button takes the disabled appearance but still routes clicks to [onClick].
 * @param iconStart Optional leading icon.
 * @param iconEnd Optional trailing icon.
 * @param loadingText Optional message shown next to the loader while [isLoading] is `true`.
 * @param contentDescription Accessibility label; overrides [text] for screen readers when non-null.
 * @param onLongClick Invoked on a long press. `null` leaves long presses unhandled.
 * @param onClick Invoked when the button is clicked.
 */
data class TangemButtonUM(
    val text: TextReference? = null,
    val variant: TangemButton.Variant = TangemButton.Variant.Primary,
    val size: TangemButton.Size = TangemButton.Size.X10,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
    val isDimmed: Boolean = false,
    val iconStart: TangemIconUM? = null,
    val iconEnd: TangemIconUM? = null,
    val loadingText: TextReference? = null,
    val contentDescription: String? = null,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
)