package com.tangem.features.tangempay.card.gpay

internal data class AddToWalletBlockState(
    val onClick: () -> Unit,
    val onClickClose: () -> Unit,
)