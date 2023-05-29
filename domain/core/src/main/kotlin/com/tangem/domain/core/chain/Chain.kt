package com.tangem.domain.core.chain

import arrow.core.Either

/**
 * A chain in the [ChainProcessor] class for processing a chain of operations with the ability to handle errors.
 * @param E the type of error
 * @param R the type of result
 */
interface Chain<E, R> {

    /**
     * Invokes the chain with the previous chain result as input and returns an [Either] result.
     * @param previousChainResult the previous chain result as an [R]
     * @return the result of the chain processing as an [Either]
     */
    suspend operator fun invoke(previousChainResult: R): Either<E, R>
}