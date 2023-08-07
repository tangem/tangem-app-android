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
 *
 * @author Andrew Khokhlov on 23/06/2023
 */
data class ActionButtonConfig(
    val text: TextReference,
    @DrawableRes val iconResId: Int,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)
