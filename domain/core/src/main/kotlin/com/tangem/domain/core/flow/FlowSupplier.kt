package com.tangem.domain.core.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

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

    /** Get first [Data] by [params] or null if [Flow] is empty */
    suspend fun getSyncOrNull(params: Params): Data? {
        return invoke(params).firstOrNull()
    }
}