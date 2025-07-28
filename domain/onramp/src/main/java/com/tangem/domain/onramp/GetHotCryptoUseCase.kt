package com.tangem.domain.onramp

import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Get hot crypto currencies
 *
 * @property hotCryptoRepository hot crypto repository
 *
[REDACTED_AUTHOR]
 */
class GetHotCryptoUseCase(
    private val hotCryptoRepository: HotCryptoRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<HotCryptoCurrency>> {
        return hotCryptoRepository.getCurrencies(userWalletId)
    }
}