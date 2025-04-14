package com.tangem.tap.domain.userWalletList.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.wallets.models.UserWalletId

@JsonClass(generateAdapter = true)
internal data class UserWalletSensitiveInformation(
    @Json(name = "wallets")
    val wallets: List<CardDTO.Wallet>,
    @Json(name = "visaCardActivationStatus")
    val visaCardActivationStatus: VisaCardActivationStatus? = null,
)

@JsonClass(generateAdapter = true)
internal data class UserWalletPublicInformation(
    @Json(name = "name")
    val name: String,
    @Json(name = "walletId")
    val walletId: UserWalletId,
    @Json(name = "artworkUrl")
    val artworkUrl: String,
    @Json(name = "cardsInWallet")
    val cardsInWallet: Set<String>,
    @Json(name = "scanResponse")
    val scanResponse: ScanResponse,
    @Json(name = "isMultiCurrency")
    val isMultiCurrency: Boolean,
    @Json(name = "hasBackupError")
    val hasBackupError: Boolean = false,
)