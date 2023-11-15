package com.tangem.feature.swap.models.states

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

class ChooseProviderBottomSheetConfig(
    val selectedProviderId: String,
    val providers: ImmutableList<ProviderState>,
) : TangemBottomSheetConfigContent