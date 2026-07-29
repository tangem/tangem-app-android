package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body of `POST v1/user-wallets/wallets/{wallet_id}/cards`.
 *
 * Reports the cards known to the app for a wallet so the backend can detect
 * an interrupted backup even after the app is reinstalled or the wallet is opened on another device.
 *
 * @property cards    cards associated with the wallet
 * @property usedSeed `true` if a seed phrase was used to create or import the wallet
 */
@JsonClass(generateAdapter = true)
@Suppress("BooleanPropertyNaming")
data class WalletCardsBody(
    @Json(name = "cards") val cards: List<WalletCardDTO>,
    @Json(name = "usedSeed") val usedSeed: Boolean,
)