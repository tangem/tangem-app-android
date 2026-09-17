package com.tangem.core.remote

import com.tangem.core.remote.config.ApiConfig

/**
 * Contract for building Retrofit API instances for a given API configuration, without exposing the
 * builder implementation. Depend on this from any module that needs a network API; the implementation
 * (and its heavy dependencies) stays in the datasource layer.
 */
interface RetrofitFactory {

    /** Builds a Retrofit API instance of [clazz] according to [spec]. */
    fun <T : Any> create(clazz: Class<T>, spec: RetrofitApiSpec): T
}

/** Reified convenience over [RetrofitFactory.create]. */
inline fun <reified T : Any> RetrofitFactory.build(spec: RetrofitApiSpec): T = create(
    clazz = T::class.java,
    spec = spec,
)

/**
 * Parameters for building a Retrofit API instance.
 *
 * @property apiConfigId                   id of the API configuration to build against
 * @property shouldApplyTimeoutAnnotations whether per-method timeout annotations are honored
 * @property shouldUseSessionAuth          whether to install the session auth interceptor/authenticator
 * @property timeouts                      optional client-level timeouts
 * @property shouldSaveLogs                whether to persist network logs
 * @property isLoggable                    whether request/response bodies may reach the debug loggers
 *   (Chucker, logcat). `false` keeps them off for this API even when its [ApiConfig] allows logging —
 *   for bodies that must not be readable anywhere, such as identity tokens. Note that
 *   [shouldSaveLogs] only governs the exportable log file, not these loggers.
 */
data class RetrofitApiSpec(
    val apiConfigId: ApiConfig.ID,
    val shouldApplyTimeoutAnnotations: Boolean,
    val shouldUseSessionAuth: Boolean,
    val timeouts: Timeouts? = null,
    val shouldSaveLogs: Boolean = true,
    val isLoggable: Boolean = true,
)

/** Optional client-level timeouts (seconds); `null` fields keep the client defaults. */
data class Timeouts(
    val callTimeoutSeconds: Long? = null,
    val connectTimeoutSeconds: Long? = null,
    val readTimeoutSeconds: Long? = null,
    val writeTimeoutSeconds: Long? = null,
)