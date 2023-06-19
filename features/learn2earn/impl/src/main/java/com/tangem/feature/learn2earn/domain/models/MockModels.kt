package com.tangem.feature.learn2earn.domain.models

import com.tangem.datasource.api.promotion.models.PromotionInfoResponse

/**
[REDACTED_AUTHOR]
 */
internal fun Promotion.Companion.dummyPending(): Promotion {
    return Promotion(
        info = Promotion.PromotionInfo.dummy().copy(
            status = PromotionInfoResponse.Status.PENDING,
        ),
        error = null,
    )
}

internal fun Promotion.Companion.dummyActive(): Promotion {
    return Promotion(
        info = Promotion.PromotionInfo.dummy().copy(
            status = PromotionInfoResponse.Status.ACTIVE,
        ),
        error = null,
    )
}

internal fun Promotion.Companion.dummyFinished(): Promotion {
    return Promotion(
        info = Promotion.PromotionInfo.dummy().copy(
            status = PromotionInfoResponse.Status.FINISHED,
        ),
        error = null,
    )
}

internal fun Promotion.Companion.dummyError(): Promotion {
    return Promotion(
        info = null,
        error = PromotionError.Error(
            code = 105,
            description = "any",
        ),
    )
}

internal fun Promotion.Companion.dummyErrorUnreachable(): Promotion {
    return Promotion(
        info = null,
        error = PromotionError.NetworkUnreachable,
    )
}

private fun Promotion.PromotionInfo.Companion.dummy(): Promotion.PromotionInfo {
    return Promotion.PromotionInfo(
        status = PromotionInfoResponse.Status.ACTIVE,
        awardForNewCard = 10f,
        awardForOldCard = 5.5f,
        awardPaymentToken = Promotion.tokenInfo(),
    )
}

private fun Promotion.Companion.tokenInfo(): PromotionInfoResponse.TokenInfo {
    return PromotionInfoResponse.TokenInfo(
        id = "1inch",
        name = "1inch",
        symbol = "1INCH",
        active = true,
        networks = listOf(
            PromotionInfoResponse.TokenInfo.Network(
                networkId = "polygon-pos",
                exchangeable = false,
                contractAddress = "0x9c2c5fd7b07e95ee044ddeba0e97a665f142394f",
                decimalCount = 1,
            ),
        ),
    )
}