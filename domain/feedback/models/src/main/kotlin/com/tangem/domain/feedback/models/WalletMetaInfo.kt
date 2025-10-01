package com.tangem.domain.feedback.models

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
data class WalletMetaInfo(
    val userWalletId: UserWalletId?,
    val hotWalletIsBackedUp: Boolean? = null,
    val cardId: String? = null,
    val firmwareVersion: String? = null,
    val cardsCount: String? = null,
    val cardBlockchain: String? = null,
    val signedHashesList: List<SignedHashes>? = null,
    val isImported: Boolean? = null,
    val isStart2Coin: Boolean? = null,
    val isVisa: Boolean? = null,
) {

    @Serializable
    data class SignedHashes(val curve: String, val total: String?)
}