package com.tangem.domain.core.chain

import arrow.core.Either
import arrow.core.raise.either

// TODO: Create Ior implementation
// TODO: Create accumulate implementation
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
    fun addChains(chains: List<Chain<E, R>>) {
        this.chains.addAll(chains)
    }

    suspend fun launchChains(initial: R): Either<E, R> {
        return either {
            chains.fold(initial) { prevChainResult, chain ->
                chain.invoke(prevChainResult).bind()
            }
        }
    }
}