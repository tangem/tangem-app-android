package com.tangem.core.remote

/**
 * Contract for building Retrofit API instances for a given API configuration, without exposing the
 * builder implementation. Depend on this from any module that needs a network API; the implementation
 * (and its heavy dependencies) stays in the datasource layer.
 */
interface RetrofitFactory {

    /**
     * Builds a Retrofit API instance of [clazz] for the API configuration identified by [configId].
     *
     * @param clazz                   the API interface to create
     * @param configId                stable id of the API configuration to use
     * @param applyTimeoutAnnotations whether per-method timeout annotations are honored
     * @param sessionAuth             whether to install the session auth interceptor/authenticator
     * @param timeouts                optional client-level timeouts
     * @param logsSaving              whether to persist network logs
     */
    fun <T : Any> create(
        clazz: Class<T>,
        configId: String,
        applyTimeoutAnnotations: Boolean,
        sessionAuth: Boolean,
        timeouts: Timeouts? = null,
        logsSaving: Boolean = true,
    ): T
}

/** Reified convenience over [RetrofitFactory.create]. */
inline fun <reified T : Any> RetrofitFactory.build(
    configId: String,
    applyTimeoutAnnotations: Boolean,
    sessionAuth: Boolean,
    timeouts: Timeouts? = null,
    logsSaving: Boolean = true,
): T = create(
    clazz = T::class.java,
    configId = configId,
    applyTimeoutAnnotations = applyTimeoutAnnotations,
    sessionAuth = sessionAuth,
    timeouts = timeouts,
    logsSaving = logsSaving,
)

/** Optional client-level timeouts (seconds); `null` fields keep the client defaults. */
data class Timeouts(
    val callTimeoutSeconds: Long? = null,
    val connectTimeoutSeconds: Long? = null,
    val readTimeoutSeconds: Long? = null,
    val writeTimeoutSeconds: Long? = null,
)