package com.tangem.lib.auth.session.internal

import arrow.core.None
import arrow.core.Option
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore

/**
 * No-op fallback used when the backend-authentication feature toggle is off
 * or the encrypted storage failed to initialise.
 */
internal object DisabledSessionTokensStore : SessionTokensStore {

    override suspend fun get(): Option<SessionTokens> = None

    override suspend fun save(tokens: SessionTokens) = Unit

    override suspend fun clear() = Unit
}