package com.tangem.domain.core.flow

import arrow.core.Either

/**
 * Flow fetcher
 *
 * @param Params data that required to fetch flow
 *
[REDACTED_AUTHOR]
 */
interface FlowFetcher<Params : Any> {

    suspend operator fun invoke(params: Params): Either<Throwable, Unit>
}