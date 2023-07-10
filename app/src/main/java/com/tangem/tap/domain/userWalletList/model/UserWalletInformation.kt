package com.tangem.tap.domain.userWalletList.model

import com.squareup.moshi.JsonClass
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWalletId

@JsonClass(generateAdapter = true)
internal data class UserWalletSensitiveInformation(
    val wallets: List<CardDTO.Wallet>,
)

@JsonClass(generateAdapter = true)
internal data class UserWalletPublicInformation(
    val name: String,
    val walletId: UserWalletId,
    val artworkUrl: String,
    val cardsInWallet: Set<String>,
    val scanResponse: ScanResponse,
    val isMultiCurrency: Boolean,
)