package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository

/**
 * Use case for getting entry info about staking on token screen.
 */
class GetStakingEntryInfoUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
        stakingOption: StakingOption,
    ): Either<StakingError, StakingEntryInfo> {
        return Either
            .catch {
                when (stakingOption) {
                    is StakingOption.StakeKit -> {
                        stakingRepository.getEntryInfo(
                            cryptoCurrencyId = cryptoCurrencyId,
                            symbol = symbol,
                        )
                    }
                    is StakingOption.P2P -> {
                        StakingEntryInfo(
                            tokenSymbol = "ETH",
                        )
                    }
                }
            }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}