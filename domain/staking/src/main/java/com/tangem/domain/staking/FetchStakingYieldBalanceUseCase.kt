package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId

class FetchStakingYieldBalanceUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
        refresh: Boolean = false,
    ): Either<StakingError, Unit> {
        return either {
            catch(
                block = {
                    stakingRepository.fetchSingleYieldBalance(
                        userWalletId = userWalletId,
                        address = address,
                        refresh = refresh,
                    )
                },
                catch = { stakingErrorResolver.resolve(it) },
            )
        }
    }
}