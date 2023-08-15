package com.tangem.core.ui.components.buttons.actions

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Action button config
 *
 * @property text      text
 * @property iconResId icon resource id
 * @property onClick   lambda be invoked when action component is clicked
 * @property enabled   enabled
 * @property dimContent determines whether the button content will be dimmed. This property will be ignored if [enabled]
 * is `false`.
 *
[REDACTED_AUTHOR]
 */
data class ActionButtonConfig(
    val text: TextReference,
    @DrawableRes val iconResId: Int,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val dimContent: Boolean = false,
)