package com.tangem.feature.wallet.presentation.wallet.state

import androidx.annotation.DrawableRes

/**
 * Action button config
 *
 * @property text      text
 * @property iconResId icon resource id
 * @property onClick   lambda be invoked when action component is clicked
 * @property enabled   enabled
 */
data class TokenActionButtonConfig(
    val text: String,
    @DrawableRes val iconResId: Int,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)