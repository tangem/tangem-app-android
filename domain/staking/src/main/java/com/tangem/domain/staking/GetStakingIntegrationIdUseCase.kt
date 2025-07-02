package com.tangem.domain.staking

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.repositories.StakingRepository

class GetStakingIntegrationIdUseCase(
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(cryptoCurrencyId: CryptoCurrency.ID) =
        stakingRepository.getSupportedIntegrationId(cryptoCurrencyId)
}