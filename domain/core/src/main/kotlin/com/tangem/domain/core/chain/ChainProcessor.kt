package com.tangem.domain.core.chain

import arrow.core.Either

// TODO: Create Ior implementation
// TODO: Create accumulate implementation
/**
 * A class for processing a chain of operations with the ability to handle errors.
 * @param E the type of error
 * @param R the type of result
 */
class ChainProcessor<E : Any, R : Any> {

    /**
     * The list of chains to be executed in order.
     */
    private val chains: MutableList<Chain<E, R>> = mutableListOf()

    /**
     * Sets the list of chains to be executed.
     * @param chains the chains to be set
     */
    fun setChains(chains: List<Chain<E, R>>) {
        this.chains.clear()
        this.chains.addAll(chains)
    }

    /**
     * Launches the chains with the [initial] value.
     * */
    suspend fun launchChains(initial: Either<E, R>): Either<E, R> {
        return chains.fold(initial) { prevChainResult, chain ->
            chain.launch(prevChainResult)
        }
    }
}