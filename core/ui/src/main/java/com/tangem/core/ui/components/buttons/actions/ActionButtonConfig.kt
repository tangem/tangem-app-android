package com.tangem.core.ui.components.buttons.actions

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Action button config
 *
 * @property text      text
 * @property iconResId icon resource id
 * @property onClick   lambda be invoked when action component is clicked
 * @property onLongClick   lambda be invoked when action component is long clicked
 * @property enabled   enabled
 * @property dimContent determines whether the button content will be dimmed. This property will be ignored if [enabled]
 * is `false`.
 * @property isInProgress indicates progress state of button
 * @property showBadge display dot in upper right corner
 *
[REDACTED_AUTHOR]
 */
data class ActionButtonConfig(
    val text: TextReference,
    @DrawableRes val iconResId: Int,
    val onClick: () -> Unit,
    val onLongClick: (() -> TextReference?)? = null,
    val enabled: Boolean = true,
    val dimContent: Boolean = false,
    val isInProgress: Boolean = false,
    val showBadge: Boolean = false,
)