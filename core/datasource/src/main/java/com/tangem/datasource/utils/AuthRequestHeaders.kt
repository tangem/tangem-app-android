package com.tangem.datasource.utils

import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.header.RequestHeader
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend

/**
 * Card authentication headers ([AuthProvider]-backed). Kept next to [AuthProvider] so [RequestHeader] in
 * core:remote stays free of any auth dependency.
 */
class AuthenticationHeader(authProvider: AuthProvider) : RequestHeader(
    "card_id" to ProviderSuspend(authProvider::getCardId),
    "card_public_key" to ProviderSuspend(authProvider::getCardPublicKey),
)

/**
 * Use ONLY for tangemApi (not express or yields)
 */
class TangemApiKeyHeader(authProvider: AuthProvider, apiEnvironment: Provider<ApiEnvironment>) : RequestHeader(
    "api-key" to authProvider.getApiKey(apiEnvironment),
)