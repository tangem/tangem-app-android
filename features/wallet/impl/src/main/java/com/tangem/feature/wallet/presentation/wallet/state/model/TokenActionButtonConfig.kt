package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Action button config
 *
 * @property text      text
 * @property iconResId icon resource id
 * @property onClick   lambda be invoked when action component is clicked
 * @property isWarning if warning row
 * @property enabled   enabled
 */
data class TokenActionButtonConfig(
    val text: TextReference,
    @DrawableRes val iconResId: Int,
    val onClick: () -> Unit,
    val isWarning: Boolean,
    val enabled: Boolean = true,
)