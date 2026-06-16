package com.tangem.domain.yield.supply.models

import kotlinx.datetime.Instant

sealed interface YieldBoostPromo {

    data object None : YieldBoostPromo

    data class Active(
        val tokens: List<PromoToken>,
        val timeline: Timeline,
        val link: String?,
    ) : YieldBoostPromo {

        data class PromoToken(
            val contractAddress: String,
            val tokenSymbol: String,
            val tokenName: String,
            val networkId: String,
        )

        data class Timeline(val start: Instant, val end: Instant)
    }
}