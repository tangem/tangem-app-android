package com.tangem.data.tokens.repository

import com.tangem.datasource.local.token.TokenReceiveWarningActionStore
import com.tangem.domain.tokens.repository.TokenReceiveWarningsViewedRepository

internal class DefaultTokenReceiveWarningsViewedRepository(
    private val tokenReceiveWarningActionStore: TokenReceiveWarningActionStore,
) : TokenReceiveWarningsViewedRepository {

    override suspend fun getViewedWarnings(): Set<String> {
        return tokenReceiveWarningActionStore.getSync()
    }

    override suspend fun view(symbol: String) {
        tokenReceiveWarningActionStore.store(symbol)
    }
}