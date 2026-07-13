package com.tangem.features.commonfeatures.impl.choosetoken.predefined.state

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class PredefinedTokensUM(
    val items: ImmutableList<PredefinedTokenItemUM>,
)

internal data class PredefinedTokenItemUM(
    val id: String,
    val symbol: String,
    val networkName: TextReference,
    val networkId: String,
    val iconUrl: String?,
    val onAddClick: () -> Unit,
)