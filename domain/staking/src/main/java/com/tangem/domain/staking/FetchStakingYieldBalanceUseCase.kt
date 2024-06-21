package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId

class FetchStakingYieldBalanceUseCase(
    private val stakingRepository: StakingRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkStatus: NetworkStatus,
        integrationId: String,
        refresh: Boolean = false,
    ): Either<Throwable, Unit> = Either.catch {
        stakingRepository.fetchSingleYieldBalance(
            userWalletId = userWalletId,
            networkStatus = networkStatus,
            integrationId = integrationId,
            refresh = refresh,
        )
    }
}