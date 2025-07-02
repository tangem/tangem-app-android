package com.tangem.features.hotwallet.addexistingwallet.start.entity

internal data class AddExistingWalletStartUM(
    val onBackClick: () -> Unit,
    val onImportPhraseClick: () -> Unit,
    val onScanCardClick: () -> Unit,
    val onBuyCardClick: () -> Unit,
)