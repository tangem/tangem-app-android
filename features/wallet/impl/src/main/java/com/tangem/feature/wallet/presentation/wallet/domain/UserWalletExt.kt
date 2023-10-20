package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.UserWallet

fun UserWallet.getCardsCount(): Int? {
    return if (isMultiCurrency) {
        when (val status = scanResponse.card.backupStatus) {
            is CardDTO.BackupStatus.Active -> status.cardCount + 1
            is CardDTO.BackupStatus.CardLinked -> status.cardCount + 1
            is CardDTO.BackupStatus.NoBackup -> 1
            null -> 1 // Multi-currency wallet without backup function. Example, 4.12
        }
    } else {
        null
    }
}