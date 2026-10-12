package com.tangem.feature.swap.models.states

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.express.models.ProviderFilterType
import kotlinx.collections.immutable.ImmutableList

data class ChooseProviderBottomSheetConfig(
    val selectedProviderId: String,
    val providers: ImmutableList<ProviderState>,
    val allProviders: ImmutableList<ProviderState>,
    val notification: NotificationUM?,
    val selectedFilter: ProviderFilterType,
    val availableFilters: ImmutableList<ProviderFilterType>,
    val onFilterSelect: (ProviderFilterType) -> Unit,
)

/** Provider rows and filters [SwapModel][com.tangem.feature.swap.model.SwapModel] keeps up to date while the sheet is open. */
data class ChooseProviderSheetSource(
    val selectedProviderId: String,
    val allProviders: ImmutableList<ProviderState>,
    val notification: NotificationUM?,
    val availableFilters: ImmutableList<ProviderFilterType>,
)