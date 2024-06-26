package com.tangem.domain.feedback.models

import com.tangem.domain.wallets.models.UserWalletId

data class CardInfo(
    val userWalletId: UserWalletId?,
    val cardId: String,
    val firmwareVersion: String,
    val cardBlockchain: String?,
    val signedHashesList: List<SignedHashes>,
    val isImported: Boolean,
    val isStart2Coin: Boolean,
) {

    data class SignedHashes(val curve: String, val total: String?)
}
