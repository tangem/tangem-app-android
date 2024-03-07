package com.tangem.domain.feedback.models

data class CardInfo(
    val userWalletId: String,
    val cardId: String,
    val firmwareVersion: String,
    val cardBlockchain: String?,
    val signedHashesList: List<SignedHashes>,
    val isStart2Coin: Boolean,
) {

    data class SignedHashes(val curve: String, val total: String?)
}