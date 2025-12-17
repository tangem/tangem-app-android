package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher

class FetchStakingYieldBalanceUseCase(
    private val singleStakingBalanceFetcher: SingleStakingBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<StakingError, Unit> = either {
        val stakingId = stakingIdFactory.create(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            network = cryptoCurrency.network,
        )
            .getOrElse {
                when (it) {
                    is StakingIdFactory.Error.UnableToGetAddress -> raise(StakingError.DomainError("$it"))
                    StakingIdFactory.Error.UnsupportedCurrency -> Unit.right()
                }

                return@either
            }

        singleStakingBalanceFetcher(
            params = SingleStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingId = stakingId),
        )
            .mapLeft { StakingError.DomainError("$it") }
            .bind()
    }
}