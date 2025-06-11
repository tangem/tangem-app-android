package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Use case for getting info about staking capability in tangem app.
 */
class GetStakingAvailabilityUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): EitherFlow<StakingError, StakingAvailability> {
        return stakingRepository.getStakingAvailability(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        ).map<StakingAvailability, Either<StakingError, StakingAvailability>> {
            it.right()
        }.catch { emit(stakingErrorResolver.resolve(it).left()) }
    }

    suspend fun invokeSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<StakingError, StakingAvailability> = Either.catch {
        stakingRepository.getStakingAvailabilitySync(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }.mapLeft(stakingErrorResolver::resolve)
}