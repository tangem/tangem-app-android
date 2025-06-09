package com.tangem.domain.feedback.models

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
data class CardInfo(
    val userWalletId: UserWalletId?,
    val cardId: String,
    val firmwareVersion: String,
    val cardsCount: String,
    val cardBlockchain: String?,
    val signedHashesList: List<SignedHashes>,
    val isImported: Boolean,
    val isStart2Coin: Boolean,
    val isVisa: Boolean,
) {

    @Serializable
    data class SignedHashes(val curve: String, val total: String?)
}