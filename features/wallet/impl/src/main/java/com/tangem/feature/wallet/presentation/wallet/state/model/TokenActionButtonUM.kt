package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.serialization.Serializable

/**
 * Action button config
 *
 * @property text      text
 * @property iconResId icon resource id
 * @property onClick   lambda be invoked when action component is clicked
 * @property isWarning if warning row
 * @property isEnabled   enabled
 */
@Stable
@Serializable
data class TokenActionButtonUM(
    val id: String,
    val text: TextReference,
    @DrawableRes val iconResId: Int,
    val onClick: () -> Unit,
    val isWarning: Boolean,
    val isEnabled: Boolean = true,
    val hasDivider: Boolean = false,
)