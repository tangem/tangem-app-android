package com.tangem.feature.swap.models.states

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
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
) : TangemBottomSheetConfigContent