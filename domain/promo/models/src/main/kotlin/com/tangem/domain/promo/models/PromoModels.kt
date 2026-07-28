package com.tangem.domain.promo.models

import kotlinx.datetime.Instant

data class PromoPayoutToken(
    val tokenId: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenName: String,
    val networkId: String,
    val decimals: Int,
)

data class PromoTimeline(
    val start: Instant,
    val end: Instant,
)

data class TokenReward(
    val tokenAddress: String,
    val networkId: String,
    val userAddress: String,
    val tokenId: String,
)

data class EnrolledTokenReward(
    val tokenAddress: String,
    val networkId: String,
    val tokenId: String,
)

sealed interface EnrollResult {
    val tokenReward: EnrolledTokenReward

    data class Success(override val tokenReward: EnrolledTokenReward) : EnrollResult
    data class AlreadyEnrolled(override val tokenReward: EnrolledTokenReward) : EnrollResult
}