package com.tangem.domain.promo

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import kotlinx.coroutines.flow.Flow

/**
 * Backend promo-campaign plumbing (eligible campaigns and registration).
 *
 * Methods other than [register] propagate the underlying error (network, parsing, etc.) by throwing rather than
 * returning an error type — callers (use cases) are expected to wrap the call, e.g. with `Either.catch`.
 */
interface PromoRepository {

    /**
     * Returns the campaigns eligible for [userWallet] (`GET /api/referral/v1/promotion/`), cached per wallet for the
     * app session unless [forceRefresh]. Throws if the fetch fails; a failure never evicts other wallets' entries.
     */
    @Throws(Exception::class)
    suspend fun getCampaignsSync(userWallet: UserWallet, forceRefresh: Boolean = false): List<PromoCampaign>

    /** Emits the cached campaigns of [userWalletId] whenever [getCampaignsSync] stores new ones; never fetches. */
    fun getCampaigns(userWalletId: UserWalletId): Flow<List<PromoCampaign>>

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