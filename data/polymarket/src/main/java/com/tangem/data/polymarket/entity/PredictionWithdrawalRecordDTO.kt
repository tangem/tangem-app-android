package com.tangem.data.polymarket.entity

import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** @property transactionId null until the BFF acknowledged the batch with a `202` */
@Serializable
internal data class PredictionWithdrawalRecordDTO(
    @SerialName("owner_address") val ownerAddress: String,
    @SerialName("deposit_wallet_address") val depositWalletAddress: String,
    @SerialName("nonce") val nonce: String,
    @SerialName("deadline") val deadline: String,
    @SerialName("target") val target: String,
    @SerialName("value") val value: String,
    @SerialName("data") val data: String,
    @SerialName("signature") val signature: String,
    @SerialName("transaction_id") val transactionId: String? = null,
)

internal fun PredictionWithdrawalRecord.toDTO(): PredictionWithdrawalRecordDTO = PredictionWithdrawalRecordDTO(
    ownerAddress = batch.ownerAddress,
    depositWalletAddress = batch.depositWalletAddress,
    nonce = batch.nonce,
    deadline = batch.deadline,
    target = batch.call.target,
    value = batch.call.value,
    data = batch.call.data,
    signature = batch.signature,
    transactionId = transactionId,
)

internal fun PredictionWithdrawalRecordDTO.toDomain(): PredictionWithdrawalRecord = PredictionWithdrawalRecord(
    batch = PredictionWithdrawalBatch(
        ownerAddress = ownerAddress,
        depositWalletAddress = depositWalletAddress,
        nonce = nonce,
        deadline = deadline,
        call = PolymarketApprovalCall(target = target, value = value, data = data),
        signature = signature,
    ),
    transactionId = transactionId,
)