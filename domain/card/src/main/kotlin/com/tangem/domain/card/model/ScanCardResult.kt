package com.tangem.domain.card.model

import com.tangem.operations.backup.PrimaryCard

data class ScanCardResult(
    val card: Card,
    val walletData: CardWalletData,
    val primaryCard: PrimaryCard,
)
