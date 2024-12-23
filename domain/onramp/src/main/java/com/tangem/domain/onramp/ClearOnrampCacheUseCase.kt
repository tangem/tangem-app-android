package com.tangem.domain.onramp

import com.tangem.domain.onramp.repositories.OnrampRepository

class ClearOnrampCacheUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke() {
        repository.clearCache()
    }
}