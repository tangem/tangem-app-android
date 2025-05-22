package com.tangem.domain.wallets.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.scan.ScanResponse
import kotlinx.serialization.Serializable

sealed interface NewUserWallet {

    val name: String
    val walletId: UserWalletId

    @JsonClass(generateAdapter = true)
    data class Cold(
        @Json(name = "name")
        override val name: String,
        @Json(name = "walletId")
        override val walletId: UserWalletId,
        @Json(name = "cardsInWallet")
        val cardsInWallet: Set<String>,
        @Json(name = "isMultiCurrency")
        val isMultiCurrency: Boolean,
        @Json(name = "hasBackupError")
        val hasBackupError: Boolean,
        @Json(name = "scanResponse")
        val scanResponse: ScanResponse,
    ) : NewUserWallet

    @JsonClass(generateAdapter = true)
    data class Hot(
        @Json(name = "name")
        override val name: String,
        @Json(name = "walletId")
        override val walletId: UserWalletId,
    ) : NewUserWallet

    @JsonClass(generateAdapter = true)
    data class Pay(
        @Json(name = "name")
        override val name: String,
        @Json(name = "walletId")
        override val walletId: UserWalletId,
        @Json(name = "cardsInWallet")
        val cardsInWallet: Set<String>,
        @Json(name = "scanResponse")
        val scanResponse: ScanResponse,
    ) : NewUserWallet
}

@JsonClass(generateAdapter = true)
data class UserWalletAccount(
    val accountId: AccountId,
    val userWalletId: UserWalletId,
    val name: String,
    val derivationPath: DerivationPath,
    val otherInfo: String,
)

@Serializable
@JvmInline
@JsonClass(generateAdapter = true)
value class AccountId(
    @Json(name = "stringValue")
    val stringValue: String,
)