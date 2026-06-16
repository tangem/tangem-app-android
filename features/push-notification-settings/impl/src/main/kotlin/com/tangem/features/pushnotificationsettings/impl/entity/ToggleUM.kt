package com.tangem.features.pushnotificationsettings.impl.entity

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class ToggleUM(
    val id: ToggleId,
    @StringRes val titleRes: Int,
    val subtitle: TextReference,
    val isOn: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

internal enum class ToggleId {
    TransactionAlerts,
    OffersUpdates,
    PriceAlerts,
}