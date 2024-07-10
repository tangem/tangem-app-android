package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.staking.model.YieldBalance
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetStakingYieldBalanceUseCase(
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
    ): EitherFlow<Throwable, YieldBalance> {
        return stakingRepository.getSingleYieldBalanceFlow(
            userWalletId = userWalletId,
            address = address,
        ).map<YieldBalance, Either<Throwable, YieldBalance>> { it.right() }
            .catch { emit(it.left()) }
    }
}