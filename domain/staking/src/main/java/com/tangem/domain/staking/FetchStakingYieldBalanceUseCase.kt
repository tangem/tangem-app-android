package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId

class FetchStakingYieldBalanceUseCase(
    private val stakingRepository: StakingRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
        refresh: Boolean = false,
    ): Either<Throwable, Unit> = Either.catch {
        stakingRepository.fetchSingleYieldBalance(
            userWalletId = userWalletId,
            address = address,
            refresh = refresh,
        )
    }
}