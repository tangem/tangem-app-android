package com.tangem.core.remote.moshi

import javax.inject.Qualifier

/**
 * Qualifies the Moshi instance configured for network (de)serialization. Lives in core:remote so any
 * module building or consuming network payloads can reference it without depending on core:datasource.
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class NetworkMoshi