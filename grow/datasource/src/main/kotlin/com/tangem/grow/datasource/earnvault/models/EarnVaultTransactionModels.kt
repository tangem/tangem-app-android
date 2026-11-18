package com.tangem.grow.datasource.earnvault.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A standalone Unwrap has no prepared batch and sends a fresh [batchId] */
@Serializable
data class EarnVaultRegisterTransactionsBody(
    @SerialName("vaultId")
    val vaultId: String,
    @SerialName("network")
    val network: String,
    @SerialName("address")
    val address: String,
    @SerialName("batchId")
    val batchId: String,
    @SerialName("feeMode")
    val feeMode: EarnVaultFeeModeDto,
    @SerialName("transactions")
    val transactions: List<EarnVaultSignedTransactionDto>? = null,
)

/** [replacesTxHash] is set on a Speed up replacement */
@Serializable
data class EarnVaultSignedTransactionDto(
    @SerialName("stepType")
    val stepType: EarnVaultStepTypeDto,
    @SerialName("txHash")
    val txHash: String,
    @SerialName("rawTransaction")
    val rawTransaction: String,
    @SerialName("nonce")
    val nonce: Long,
    @SerialName("replacesTxHash")
    val replacesTxHash: String? = null,
)

/** [txHash] is the hash the gasless service returned for a batch reported by its `batchId` */
@Serializable
data class EarnVaultUpdateTransactionBody(
    @SerialName("network")
    val network: String,
    @SerialName("sendResult")
    val sendResult: EarnVaultSendResultDto,
    @SerialName("error")
    val error: String? = null,
    @SerialName("txHash")
    val txHash: String? = null,
)

@Serializable
data class EarnVaultTransactionsResponse(
    @SerialName("items")
    val items: List<EarnVaultTransactionDto>,
    @SerialName("nextCursor")
    val nextCursor: String?,
)

/**
 * A batch signed in one tap is one transaction with the status of its main step. [expectedBy] is set on
 * withdrawals only, [receivedAmount] once a withdrawal completes, [txHash] stays `null` on a gasless batch until
 * the gasless service returns it.
 */
@Serializable
data class EarnVaultTransactionDto(
    @SerialName("id")
    val id: String,
    @SerialName("vaultId")
    val vaultId: String,
    @SerialName("network")
    val network: String,
    @SerialName("type")
    val type: EarnVaultTransactionTypeDto = EarnVaultTransactionTypeDto.UNKNOWN,
    @SerialName("amount")
    val amount: String,
    @SerialName("receivedAmount")
    val receivedAmount: String?,
    @SerialName("status")
    val status: EarnVaultTransactionStatusDto = EarnVaultTransactionStatusDto.UNKNOWN,
    @SerialName("txHash")
    val txHash: String?,
    @SerialName("submittedAt")
    val submittedAt: String,
    @SerialName("confirmedAt")
    val confirmedAt: String?,
    @SerialName("completedAt")
    val completedAt: String?,
    @SerialName("expectedBy")
    val expectedBy: String?,
    @SerialName("errorCode")
    val errorCode: EarnVaultTransactionErrorCodeDto? = null,
)

@Serializable
enum class EarnVaultFeeModeDto {
    @SerialName("eth")
    ETH,

    @SerialName("gasless")
    GASLESS,
}

@Serializable
enum class EarnVaultSendResultDto {
    @SerialName("sent")
    SENT,

    @SerialName("failed")
    FAILED,
}

@Serializable
enum class EarnVaultTransactionTypeDto {
    @SerialName("deposit")
    DEPOSIT,

    @SerialName("withdraw")
    WITHDRAW,

    @SerialName("unwrap")
    UNWRAP,

    /** Not a wire value: a type this build does not know is coerced to this default by `NetworkJsonFactory` */
    UNKNOWN,
}

/** [REPLACED] is internal to the backend and never shown */
@Serializable
enum class EarnVaultTransactionStatusDto {
    @SerialName("submitted")
    SUBMITTED,

    @SerialName("withdrawal_in_progress")
    WITHDRAWAL_IN_PROGRESS,

    @SerialName("completed")
    COMPLETED,

    @SerialName("returned")
    RETURNED,

    @SerialName("failed")
    FAILED,

    @SerialName("replaced")
    REPLACED,

    /** Not a wire value: a status this build does not know is coerced to this default by `NetworkJsonFactory` */
    UNKNOWN,
}

@Serializable
enum class EarnVaultTransactionErrorCodeDto {
    DROPPED,
    NOT_SENT,
}