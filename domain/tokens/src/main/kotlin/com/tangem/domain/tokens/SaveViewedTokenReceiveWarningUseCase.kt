package com.tangem.domain.tokens

import com.tangem.domain.tokens.repository.TokenReceiveWarningsViewedRepository

class SaveViewedTokenReceiveWarningUseCase(
    private val tokenReceiveWarningsViewedRepository: TokenReceiveWarningsViewedRepository,
) {
    suspend operator fun invoke(symbol: String) {
        tokenReceiveWarningsViewedRepository.view(symbol)
    }
}