package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetStakingYieldBalanceUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
    ): EitherFlow<StakingError, YieldBalance> {
        return stakingRepository.getSingleYieldBalanceFlow(
            userWalletId = userWalletId,
            address = address,
        ).map<YieldBalance, Either<StakingError, YieldBalance>> { it.right() }
            .catch { emit(stakingErrorResolver.resolve(it).left()) }
    }
}