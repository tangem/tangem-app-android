package com.tangem.tap.domain.userWalletList.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId

@JsonClass(generateAdapter = true)
internal data class UserWalletSensitiveInformation(
    // Cold
    @Json(name = "wallets")
    val wallets: List<CardDTO.Wallet>?,
    @Json(name = "visaCardActivationStatus")
    val visaCardActivationStatus: VisaCardActivationStatus? = null,
    // Hot
    @Json(name = "mobileWallets")
    val mobileWallets: List<MobileWallet>? = null,
)

@JsonClass(generateAdapter = true)
internal data class UserWalletPublicInformation(
    // Common
    @Json(name = "name")
    val name: String,
    @Json(name = "walletId")
    val walletId: UserWalletId,
    // Cold
    @Json(name = "cardsInWallet")
    val cardsInWallet: Set<String>,
    @Json(name = "scanResponse")
    val scanResponse: ScanResponse?,
    @Json(name = "isMultiCurrency")
    val isMultiCurrency: Boolean,
    @Json(name = "hasBackupError")
    val hasBackupError: Boolean = false,
    // Hot
    @Json(name = "hotWalletId")
    val hotWalletId: HotWalletId? = null,
    @Json(name = "backedUp")
    val backedUp: Boolean? = null,
)