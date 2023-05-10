package com.tangem.domain.core.chain

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import arrow.core.toEitherNel

/**
 * A class for processing a chain of operations with the ability to handle errors.
 * @param E the type of error
 * @param R the type of result
 */
class ChainProcessor<E, R> {

    /**
     * The list of chains to be executed in order.
     */
    private val chains: MutableList<Chain<E, R>> = mutableListOf()

    /**
     * Adds chains to the existing list of chains to be executed.
     * @param chains the chains to be added to the list
     */
    fun addChains(vararg chains: Chain<E, R>) {
        this.chains.addAll(chains)
    }

    /**
     * Launches the chains with the given initial value as input.
     * @param initial the initial value to start the chain processing.
     * @param accumulateExceptions determines whether to accumulate exceptions or break on first exception.
     * Defaults to true.
     * @return the result of the chain processing as an EitherNel, which is a disjunction that may contain multiple errors.
     */
    private suspend fun launchChains(initial: Either<E, R>, accumulateExceptions: Boolean = true): EitherNel<E, R> {
        return chains.fold(initial.toEitherNel()) { previousChainResult, chain ->
            chain.invoke(previousChainResult)
                .mapLeft { e ->
                    previousChainResult.leftOrNull()?.plus(e)
                        ?: e.nel()
                }
                .onLeft { e ->
                    if (!accumulateExceptions) {
                        return e.left()
                    }
                }
        }
    }

    /**
     * Launches the chains with the given initial value as input.
     * This function is a convenience function that assumes the initial value does not contain any errors.
     * @param initial the initial value to start the chain processing.
     * @param accumulateExceptions determines whether to accumulate exceptions or break on first exception.
     * Defaults to true.
     * @return the result of the chain processing as an EitherNel, which is a disjunction that may contain multiple errors.
     */
    suspend fun launchChains(initial: R, accumulateExceptions: Boolean = true): EitherNel<E, R> {
        return launchChains(initial.right(), accumulateExceptions)
    }
}