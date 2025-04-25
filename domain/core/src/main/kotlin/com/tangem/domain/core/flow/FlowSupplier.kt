package com.tangem.domain.core.flow

import kotlinx.coroutines.flow.Flow

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
}