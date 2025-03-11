package com.tangem.domain.onramp

import com.tangem.domain.onramp.repositories.HotCryptoRepository

/**
 * Fetch hot crypto use case
 *
 * @property hotCryptoRepository hot crypto repository
 *
[REDACTED_AUTHOR]
 */
class FetchHotCryptoUseCase(
    private val hotCryptoRepository: HotCryptoRepository,
) {

    operator fun invoke() {
        hotCryptoRepository.fetchHotCrypto()
    }
}