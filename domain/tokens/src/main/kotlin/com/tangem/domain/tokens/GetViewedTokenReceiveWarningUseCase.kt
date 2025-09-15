package com.tangem.domain.tokens

import com.tangem.domain.tokens.repository.TokenReceiveWarningsViewedRepository

class GetViewedTokenReceiveWarningUseCase(
    private val tokenReceiveWarningsViewedRepository: TokenReceiveWarningsViewedRepository,
) {
    suspend operator fun invoke(): Set<String> {
        return tokenReceiveWarningsViewedRepository.getViewedWarnings()
    }
}