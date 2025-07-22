package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import com.tangem.domain.models.wallet.UserWalletId

class FetchStakingYieldBalanceUseCase(
    private val stakingErrorResolver: StakingErrorResolver,
    private val singleYieldBalanceFetcher: SingleYieldBalanceFetcher,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<StakingError, Unit> {
        return either {
            catch(
                block = {
                    singleYieldBalanceFetcher(
                        params = SingleYieldBalanceFetcher.Params(
                            userWalletId = userWalletId,
                            currencyId = cryptoCurrency.id,
                            network = cryptoCurrency.network,
                        ),
                    )
                },
                catch = { stakingErrorResolver.resolve(it) },
            )
        }
    }
}