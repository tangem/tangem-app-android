package com.tangem.domain.core.chain

import arrow.core.Either

interface Chain<E, R> {
    suspend operator fun invoke(previousChainResult: Either<E, R>): Either<E, R>
}
