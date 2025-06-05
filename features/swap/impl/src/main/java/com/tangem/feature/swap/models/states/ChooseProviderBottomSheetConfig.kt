package com.tangem.feature.swap.models.states

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

data class ChooseProviderBottomSheetConfig(
    val selectedProviderId: String,
    val providers: ImmutableList<ProviderState>,
    val notification: NotificationUM?,
) : TangemBottomSheetConfigContent