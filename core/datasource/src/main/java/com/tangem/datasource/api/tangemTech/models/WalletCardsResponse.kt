package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET v1/user-wallets/wallets/{wallet_id}/cards`.
 *
 * Cards known to the backend for a wallet and the state of its backup. Used to detect an interrupted backup
 * when the app has no local information about it — a new device, a reinstall or cleared app data.
 *
 * @property cards cards associated with the wallet, empty if the backend has no data about the wallet
 */
@JsonClass(generateAdapter = true)
data class WalletCardsResponse(
    @Json(name = "cards") val cards: List<WalletCardDTO> = emptyList(),
)