package com.tangem.domain.core.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [Flow] supplier
 *
 * @param Params type of data that required to get [Flow]
 * @param Data   data type of [Flow]
 *
[REDACTED_AUTHOR]
 */
interface FlowSupplier<Params : Any, Data : Any> {

    /** Supply [Flow] by [params] */
    operator fun invoke(params: Params): Flow<Data>

    /** Synchronously get first value or null from [Flow] within [timeMillis] */
    suspend fun getSyncOrNull(params: Params, timeMillis: Long? = null): Data? {
        val block = suspend { invoke(params).firstOrNull() }

        return if (timeMillis == null) {
            block()
        } else {
            withTimeoutOrNull(timeMillis) { block() }
        }
    }
}