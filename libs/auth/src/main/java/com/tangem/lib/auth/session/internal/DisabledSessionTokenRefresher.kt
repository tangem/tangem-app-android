package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.left
import com.tangem.lib.auth.session.SessionRefreshError
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_15438_BACKEND_AUTHENTICATION_ENABLED")
internal object DisabledSessionTokenRefresher : SessionTokenRefresher {

    override suspend fun refresh(): Either<SessionRefreshError, SessionTokens> {
        return SessionRefreshError.Disabled.left()
    }
}