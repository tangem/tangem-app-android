package com.tangem.grow.datasource.earnvault.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EarnVaultPositionsResponse(
    @SerialName("items")
    val items: List<EarnVaultPositionDto>,
)

/** [wrappedOwed] is the wrapped token vault flows left in the wallet, capped at its balance */
@Serializable
data class EarnVaultPositionDto(
    @SerialName("vaultId")
    val vaultId: String,
    @SerialName("network")
    val network: String,
    @SerialName("balance")
    val balance: EarnVaultMoneyDto,
    @SerialName("pendingWithdraw")
    val pendingWithdraw: EarnVaultMoneyDto,
    @SerialName("earned")
    val earned: EarnVaultMoneyDto,
    @SerialName("wrappedOwed")
    val wrappedOwed: EarnVaultMoneyDto,
    @SerialName("unitsLockedUntil")
    val unitsLockedUntil: String?,
    @SerialName("asOf")
    val asOf: String,
    @SerialName("stale")
    val isStale: Boolean,
)

@Serializable
data class EarnVaultMoneyDto(
    @SerialName("amount")
    val amount: String,
    @SerialName("usd")
    val usd: String,
)