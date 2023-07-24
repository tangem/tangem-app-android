package com.tangem.domain.core.raise

import arrow.core.raise.Raise

abstract class DelegatedRaise<Error, OtherError>(
    private val otherRaise: Raise<OtherError>,
    private val transformError: (Error) -> OtherError,
) : Raise<Error> {

    override fun raise(r: Error): Nothing {
        otherRaise.raise(transformError(r))
    }
}