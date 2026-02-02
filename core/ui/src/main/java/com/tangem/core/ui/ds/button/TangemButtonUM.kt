package com.tangem.core.ui.ds.button

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Data model representing the properties of a Tangem button.
 *
 * @param text               TextReference for the button label.
 * @param descriptionText    TextReference for the button description (optional).
 * @param iconRes            Drawable resource ID for the icon to be displayed in the button (optional).
 * @param iconPosition       Position of the icon (Start or End).
 * @param isEnabled            Boolean indicating whether the button is enabled.
 * @param size               TangemButtonSize defining the size of the button.
 * @param state              TangemButtonState defining the current state of the button.
 * @param shape              TangemButtonShape defining the shape of the button.
 * @param type               TangemButtonType defining the style type of the button.
 * @param onClick            Lambda to be invoked when the button is clicked.
 *
[REDACTED_AUTHOR]
 */
data class TangemButtonUM(
    val text: TextReference? = null,
    val descriptionText: TextReference? = null,
    @DrawableRes val iconRes: Int? = null,
    val iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    val isEnabled: Boolean = true,
    val size: TangemButtonSize = TangemButtonSize.X15,
    val state: TangemButtonState = TangemButtonState.Default,
    val shape: TangemButtonShape = TangemButtonShape.Default,
    val type: TangemButtonType,
    val onClick: () -> Unit,
)

/** Enum class representing the style types of Tangem buttons */
enum class TangemButtonType {
    Primary,
    Secondary,
    Accent,
    Outline,
    PrimaryInverse,
    Ghost,
}