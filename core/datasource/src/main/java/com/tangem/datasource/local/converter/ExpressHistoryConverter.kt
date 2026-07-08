package com.tangem.datasource.local.converter

import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity

/**
 * Maps API history items into their persisted [androidx.room.Entity] representations.
 */
fun ExchangeItemResponse.toEntity(): ExpressExchangeEntity? {
    // Items with no fromAddress (very old app versions didn't send it) can't be found by the outgoing-swap
    // lookup, which keys on from_address — drop them. Such items are effectively nonexistent nowadays.
    if (fromAddress == null) return null
    return ExpressExchangeEntity(
        txId = txId,
        providerId = providerId,
        fromAddress = fromAddress,
        payinAddress = payinAddress,
        payinExtraId = payinExtraId,
        payoutAddress = payoutAddress,
        refundAddress = refundAddress,
        refundExtraId = refundExtraId,
        rateType = rateType,
        status = status,
        externalTxId = externalTxId,
        externalTxUrl = externalTxUrl,
        payinHash = payinHash,
        payoutHash = payoutHash,
        refundNetwork = refundNetwork,
        refundContractAddress = refundContractAddress,
        createdAt = createdAt,
        updatedAt = updatedAt,
        payTill = payTill,
        averageDuration = averageDuration,
        from = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = fromContractAddress,
            network = fromNetwork,
            decimals = fromDecimals,
            amount = fromAmount,
            actualAmount = null,
        ),
        to = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = toContractAddress,
            network = toNetwork,
            decimals = toDecimals,
            amount = toAmount,
            actualAmount = toActualAmount,
        ),
    )
}

fun OnrampItemResponse.toEntity(): ExpressOnrampEntity {
    return ExpressOnrampEntity(
        txId = txId,
        providerId = providerId,
        payoutAddress = payoutAddress,
        status = status,
        failReason = failReason,
        externalTxId = externalTxId,
        externalTxUrl = externalTxUrl,
        payoutHash = payoutHash,
        createdAt = createdAt,
        updatedAt = updatedAt,
        fromCurrencyCode = fromCurrencyCode,
        fromAmount = fromAmount,
        fromPrecision = fromPrecision,
        to = ExpressOnrampEntity.AssetEmbedded(
            contractAddress = toContractAddress,
            network = toNetwork,
            decimals = toDecimals,
            amount = toAmount,
            actualAmount = toActualAmount,
        ),
        paymentMethod = paymentMethod,
        countryCode = countryCode,
    )
}