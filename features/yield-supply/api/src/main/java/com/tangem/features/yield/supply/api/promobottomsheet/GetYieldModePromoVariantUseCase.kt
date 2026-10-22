package com.tangem.features.yield.supply.api.promobottomsheet

/**
 * Reading an assigned variant tracks its exposure, so this is the last check before the promo is shown.
 * A closed release gate answers [YieldModePromoVariant.CONTROL] without reading it, and so does a device with no
 * variants yet: it does not wait for the first fetch.
 */
interface GetYieldModePromoVariantUseCase {

    suspend operator fun invoke(): YieldModePromoVariant
}

enum class YieldModePromoVariant {

    /** The holdout, an unknown value or no assignment at all: no promo. */
    CONTROL,

    /** The promo with the best APY among the wallet's tokens. */
    USER_MAX_APY,

    /** The promo with the best APY among all Yield Mode offers. */
    GLOBAL_MAX_APY,
}