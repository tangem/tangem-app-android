package com.tangem.data.wallets.converters

import com.tangem.data.wallets.store.PendingWalletCardsBackup
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.datasource.api.tangemTech.models.WalletCardsBody

/**
 * Maps between the queued form of a cards-backup report and the body that is actually POSTed.
 *
 * The two shapes are deliberately separate types: the queued one is persisted and has to keep decoding across
 * app updates, while the request DTO is free to follow the backend.
 */
internal object PendingWalletCardsBackupConverter {

    fun convert(cards: List<WalletCardDTO>): List<PendingWalletCardsBackup.Card> = cards.map { card ->
        PendingWalletCardsBackup.Card(
            cardId = card.cardId,
            cardPublicKey = card.cardPublicKey,
            role = card.role,
            backupStatus = card.backupStatus,
            curves = card.curves,
            errorCode = card.errorCode,
            errorMessage = card.errorMessage,
        )
    }

    fun convertBack(pending: PendingWalletCardsBackup): WalletCardsBody = WalletCardsBody(
        cards = pending.cards.map { card ->
            WalletCardDTO(
                cardId = card.cardId,
                cardPublicKey = card.cardPublicKey,
                role = card.role,
                backupStatus = card.backupStatus,
                curves = card.curves,
                errorCode = card.errorCode,
                errorMessage = card.errorMessage,
            )
        },
        usedSeed = pending.usedSeed,
    )
}