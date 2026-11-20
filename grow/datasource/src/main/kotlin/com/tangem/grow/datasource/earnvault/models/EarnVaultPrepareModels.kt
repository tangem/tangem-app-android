package com.tangem.grow.datasource.earnvault.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EarnVaultDepositPrepareBody(
    @SerialName("vaultId")
    val vaultId: String,
    @SerialName("network")
    val network: String,
    @SerialName("address")
    val address: String,
    @SerialName("amount")
    val amount: String,
)

@Serializable
class EarnVaultWithdrawPrepareBody private constructor(
    @SerialName("vaultId")
    val vaultId: String,
    @SerialName("network")
    val network: String,
    @SerialName("address")
    val address: String,
    @SerialName("amount")
    val amount: String? = null,
    @SerialName("entire")
    val isEntire: Boolean? = null,
) {

    companion object {

        fun partial(vaultId: String, network: String, address: String, amount: String) = EarnVaultWithdrawPrepareBody(
            vaultId = vaultId,
            network = network,
            address = address,
            amount = amount,
        )

        fun entire(vaultId: String, network: String, address: String) = EarnVaultWithdrawPrepareBody(
            vaultId = vaultId,
            network = network,
            address = address,
            isEntire = true,
        )
    }
}

/** A withdrawal fills [expectedAmount] and [minAmount]; [unwrapAmount] comes with an instant one on a wrapNative market */
@Serializable
data class EarnVaultPrepareResponse(
    @SerialName("batchId")
    val batchId: String,
    @SerialName("mode")
    val mode: EarnVaultModeDto = EarnVaultModeDto.UNKNOWN,
    @SerialName("steps")
    val steps: List<EarnVaultStepDto>,
    @SerialName("expectedAmount")
    val expectedAmount: String? = null,
    @SerialName("minAmount")
    val minAmount: String? = null,
    @SerialName("unwrapAmount")
    val unwrapAmount: String? = null,
)

@Serializable
data class EarnVaultStepDto(
    @SerialName("index")
    val index: Int,
    @SerialName("chainFamily")
    val chainFamily: EarnVaultChainFamilyDto = EarnVaultChainFamilyDto.UNKNOWN,
    @SerialName("type")
    val type: EarnVaultStepTypeDto = EarnVaultStepTypeDto.UNKNOWN,
    @SerialName("to")
    val to: String,
    @SerialName("data")
    val data: String,
    @SerialName("value")
    val value: String,
    @SerialName("gasLimit")
    val gasLimit: String,
    @SerialName("chainId")
    val chainId: Int,
)

/** A queued withdrawal is settled later by Gauntlet, within the market's `withdrawalHours` */
@Serializable
enum class EarnVaultModeDto {
    @SerialName("instant")
    INSTANT,

    @SerialName("queued")
    QUEUED,

    /** Not a wire value: a mode this build does not know is coerced to this default by `NetworkJsonFactory` */
    UNKNOWN,
}

@Serializable
enum class EarnVaultStepTypeDto {
    @SerialName("wrap")
    WRAP,

    @SerialName("approve")
    APPROVE,

    @SerialName("deposit")
    DEPOSIT,

    @SerialName("redeem")
    REDEEM,

    @SerialName("withdraw")
    WITHDRAW,

    @SerialName("requestRedeem")
    REQUEST_REDEEM,

    @SerialName("unwrap")
    UNWRAP,

    /** Not a wire value: a step this build does not know is coerced to this default by `NetworkJsonFactory` */
    UNKNOWN,
}