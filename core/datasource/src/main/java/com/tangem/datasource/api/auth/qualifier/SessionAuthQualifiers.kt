package com.tangem.datasource.api.auth.qualifier

import javax.inject.Qualifier

/**
 * Marks the OkHttp [okhttp3.Interceptor] that attaches Tangem Auth Service session credentials
 * (`Authorization: DPoP <access-token>` + `DPoP: <proof-jwt>`) to outgoing requests. The actual
 * binding lives in `libs:auth` so this module does not depend on the auth library; Hilt assembles
 * the binding at the `:app` level.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionAuthInterceptor

/**
 * Marks the OkHttp [okhttp3.Authenticator] that rotates session tokens on 401/403 responses.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionAuthAuthenticator