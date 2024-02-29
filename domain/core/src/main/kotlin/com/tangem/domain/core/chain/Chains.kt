package com.tangem.domain.core.chain

import arrow.core.Either
import arrow.core.flatMap

/**
 * A chain in the [ChainProcessor] class for processing a chain of operations with the ability to handle errors.
 * @param E the type of error
 * @param R the type of result
 */
interface Chain<E, R> {

    /**
     * Invokes the chain with the previous chain result as input and returns an [Either] result.
     *
     * @param previousChainResult the previous chain result as an [Either]
     * @return the result of the chain processing as an [Either]
     */
    suspend fun launch(previousChainResult: Either<E, R>): Either<E, R>
}

/**
 * A chain in the [ChainProcessor] class for processing a chain of operations with the ability to handle errors.
 *
 * Handles only the right side of the previous chain result.
 *
 * @param E the type of error
 * @param R the type of result
 */
abstract class FlatMapChain<E, R> : Chain<E, R> {

    override suspend fun launch(previousChainResult: Either<E, R>): Either<E, R> {
        return previousChainResult.flatMap { launch(it) }
    }

    /**
     * Invokes the chain with the previous chain result as input and returns a result.
     *
     * @param previousChainResult the previous chain result as right side of [Either]
     * @return the result of the chain processing as an [Either]
     */
    abstract suspend fun launch(previousChainResult: R): Either<E, R>
}
