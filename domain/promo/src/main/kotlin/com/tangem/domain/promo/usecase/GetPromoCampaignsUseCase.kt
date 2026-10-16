package com.tangem.domain.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoCampaign
import kotlinx.coroutines.flow.Flow

class GetPromoCampaignsUseCase(
    private val repository: PromoRepository,
) {

    /** Later campaigns of [userWallet], e.g. refreshed after a registration; the current ones come from [invokeSync]. */
    operator fun invoke(userWallet: UserWallet): Flow<List<PromoCampaign>> = repository.getCampaigns(
        userWallet.walletId,
    )

    suspend fun invokeSync(
        userWallet: UserWallet,
        forceRefresh: Boolean = false,
    ): Either<Throwable, List<PromoCampaign>> = Either.catch {
        repository.getCampaignsSync(userWallet, forceRefresh)
    }
}