package com.tangem.core.remote

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
 * @property configId                stable id of the API configuration to use
 * @property applyTimeoutAnnotations whether per-method timeout annotations are honored
 * @property sessionAuth             whether to install the session auth interceptor/authenticator
 * @property timeouts                optional client-level timeouts
 * @property logsSaving              whether to persist network logs
 */
data class RetrofitApiSpec(
    val configId: String,
    val applyTimeoutAnnotations: Boolean,
    val sessionAuth: Boolean,
    val timeouts: Timeouts? = null,
    val logsSaving: Boolean = true,
)

/** Optional client-level timeouts (seconds); `null` fields keep the client defaults. */
data class Timeouts(
    val callTimeoutSeconds: Long? = null,
    val connectTimeoutSeconds: Long? = null,
    val readTimeoutSeconds: Long? = null,
    val writeTimeoutSeconds: Long? = null,
)