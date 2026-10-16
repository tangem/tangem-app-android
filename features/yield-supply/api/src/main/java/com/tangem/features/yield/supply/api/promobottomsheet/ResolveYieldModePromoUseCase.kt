package com.tangem.features.yield.supply.api.promobottomsheet

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnYieldOffers
import java.math.BigDecimal

/** Prepares the offers of both experiment arms, so the variant is read only after the eligibility check. */
interface ResolveYieldModePromoUseCase {

    /**
     * [statuses] are the selected wallet's personal account statuses; [yieldApys] is the catalog refreshed in this
     * launch: yield supply key → effective APY, percent.
     */
    operator fun invoke(
        statuses: List<CryptoCurrencyStatus>,
        yieldApys: Map<String, BigDecimal>,
        earnOffers: EarnYieldOffers,
    ): YieldModePromoResolution
}

sealed interface YieldModePromoResolution {

    /** A matched token still has its balance or Yield Mode state loading. */
    data object Waiting : YieldModePromoResolution

    data object NoEligibleToken : YieldModePromoResolution

    /** The Earn list has no usable top offer for the global arm. */
    data object DataNotReady : YieldModePromoResolution

    data class Ready(
        val userMaxApy: YieldModePromoOffer,
        val globalMaxApy: YieldModePromoOffer,
    ) : YieldModePromoResolution {

        fun offerFor(variant: YieldModePromoVariant): YieldModePromoOffer? = when (variant) {
            YieldModePromoVariant.USER_MAX_APY -> userMaxApy
            YieldModePromoVariant.GLOBAL_MAX_APY -> globalMaxApy
            YieldModePromoVariant.CONTROL -> null
        }
    }
}