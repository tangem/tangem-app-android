package com.tangem.features.swap.v2.impl.chooseprovider.entity

import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.ImmutableList

internal data class SwapChooseProviderBottomSheetContent(
    val providerList: ImmutableList<SwapProviderListItem>,
    val selectedProvider: ExpressProvider,
)

internal data class SwapProviderListItem(
    val providerUM: ProviderChooseUM,
    val quote: SwapQuoteUM,
)