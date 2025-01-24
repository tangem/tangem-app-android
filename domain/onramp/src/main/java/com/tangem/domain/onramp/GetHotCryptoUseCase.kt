package com.tangem.domain.onramp

import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Get hot crypto currencies
 *
 * @property hotCryptoRepository hot crypto repository
 *
 * @author Andrew Khokhlov on 20/01/2025
 */
class GetHotCryptoUseCase(
    private val hotCryptoRepository: HotCryptoRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<HotCryptoCurrency>> {
        return hotCryptoRepository.getCurrencies(userWalletId)
    }
}
