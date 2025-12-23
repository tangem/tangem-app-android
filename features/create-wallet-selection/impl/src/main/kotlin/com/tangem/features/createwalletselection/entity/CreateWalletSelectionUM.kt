package com.tangem.features.createwalletselection.entity

import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class CreateWalletSelectionUM(
    val isScanInProgress: Boolean = false,
    val shouldShowAlreadyHaveWallet: Boolean = false,
    val blocks: ImmutableList<Block>,
    val onBackClick: () -> Unit,
    val onBuyClick: () -> Unit,
    val onWhatToChooseClick: () -> Unit,
) {
    data class Block(
        val title: TextReference,
        val titleLabel: LabelUM?,
        val description: TextReference,
        val onClick: () -> Unit,
    )
}