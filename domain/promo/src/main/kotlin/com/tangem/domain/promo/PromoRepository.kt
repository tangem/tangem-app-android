package com.tangem.domain.promo

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.models.TokenReward

/**
 * Backend promo-campaign plumbing (enrollment state and registration).
 *
 * Both methods propagate the underlying error (network, parsing, etc.) by throwing rather than
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
}