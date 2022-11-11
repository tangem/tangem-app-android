package com.tangem.domain.common.util

import com.tangem.domain.common.CardDTO

val CardDTO.userWalletId: UserWalletId
    get() = UserWalletId(findWalletPublicKey(wallets))

private fun findWalletPublicKey(wallets: List<CardDTO.Wallet>): ByteArray {
    return wallets.firstOrNull()
        ?.publicKey
        ?: error("Wallet not found")
}