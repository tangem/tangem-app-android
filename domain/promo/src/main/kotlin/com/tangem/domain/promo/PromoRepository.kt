package com.tangem.domain.promo

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.TokenReward

/**
 * Backend promo-campaign plumbing (enrollment state and registration).
 *
 * Methods other than [register] propagate the underlying error (network, parsing, etc.) by throwing rather than
 * returning an error type — callers (use cases) are expected to wrap the call, e.g. with `Either.catch`.
 */
interface PromoRepository {

    /**
     * Resolves the state of [campaign] for [userWalletId]: locally enrolled, available, or not active.
     * Throws if the campaign list can't be fetched and no cached/local data is available.
     */
    @Throws(Exception::class)
    suspend fun getCampaignState(
        campaign: PromoCampaignId,
        userWalletId: UserWalletId,
        forceRefresh: Boolean = false,
    ): PromoCampaignState

    /**
     * Registers [walletIds] for [campaign] with the given [tokenReward]. Throws on any non-conflict
     * API error; a 409 conflict resolves to [EnrollResult.AlreadyEnrolled] instead of throwing.
     */
    @Throws(Exception::class)
    suspend fun enroll(
        campaign: PromoCampaignId,
        tokenReward: TokenReward,
        walletIds: List<UserWalletId>,
    ): EnrollResult

    /**
     * Returns the campaigns eligible for [userWallet] (`GET /v3/promotion`), cached per wallet for the app
     * session unless [forceRefresh]. Throws if the fetch fails; a failure never evicts other wallets' entries.
     */
    @Throws(Exception::class)
    suspend fun getCampaigns(userWallet: UserWallet, forceRefresh: Boolean = false): List<PromoCampaign>

    /**
     * Registers [enrollments] for the campaign [promotionId] (`POST /api/referral/v1/promotion/{id}/registrations`)
     * and returns a result per wallet; a wallet already registered is a result, not an error. Unlike the other
     * methods it never throws: every failure, including empty or repeated [enrollments], is a [PromoRegistrationError].
     */
    suspend fun register(
        promotionId: Int,
        enrollments: List<PromoEnrollment>,
    ): Either<PromoRegistrationError, List<PromoRegistrationResult>>
}