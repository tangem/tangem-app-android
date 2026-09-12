package com.tangem.datasource.remote.header

import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.header.RequestHeader
import com.tangem.datasource.remote.auth.AuthProvider
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend

/**
 * Card authentication headers ([AuthProvider]-backed). Auth-backed [RequestHeader]s live in datasource so
 * [RequestHeader] in core:remote stays free of any auth dependency.
 */
internal class AuthenticationHeader(authProvider: AuthProvider) : RequestHeader(
    "card_id" to ProviderSuspend(authProvider::getCardId),
    "card_public_key" to ProviderSuspend(authProvider::getCardPublicKey),
)

/**
 * Use ONLY for tangemApi (not express or yields)
 */
internal class TangemApiKeyHeader(authProvider: AuthProvider, apiEnvironment: Provider<ApiEnvironment>) : RequestHeader(
    "api-key" to authProvider.getApiKey(apiEnvironment),
)