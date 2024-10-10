package com.tangem.domain.staking

import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency

class GetStakingIntegrationIdUseCase(
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(cryptoCurrencyId: CryptoCurrency.ID) =
        stakingRepository.getSupportedIntegrationId(cryptoCurrencyId)
}