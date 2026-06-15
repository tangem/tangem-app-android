package com.tangem.tap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Persisted entry of an app-initiated sell (off-ramp) flow, stored in a dedicated kotlinx-serialized DataStore.
 *
 * [userWalletId] holds the [com.tangem.domain.models.wallet.UserWalletId.stringValue].
 *
 * @see com.tangem.domain.offramp.model.PendingOfframp
 */
@Serializable
internal data class PendingOfframpEntry(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("userWalletId")
    val userWalletId: String,
    @SerialName("currencyId")
    val currencyId: String,
    @SerialName("createdAt")
    val createdAt: Long,
)