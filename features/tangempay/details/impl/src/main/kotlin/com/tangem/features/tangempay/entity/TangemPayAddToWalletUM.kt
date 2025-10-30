package com.tangem.features.tangempay.entity

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayAddToWalletUM(
    val steps: ImmutableList<TangemPayAddToWalletStepItemUM>,
    val showAddToWalletButton: Boolean,
    val onBackClick: () -> Unit,
    val onClickOpenWallet: () -> Unit,
)

internal data class TangemPayAddToWalletStepItemUM(
    val count: Int,
    val text: TextReference,
)