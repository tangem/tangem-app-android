package com.tangem.domain.core.chain

import arrow.core.Either

class ChainProcessor<E, R> {
    private val chains: MutableList<Chain<E, R>> = mutableListOf()

    fun addChains(vararg chains: Chain<E, R>) {
        this.chains.addAll(chains)
    }

    suspend fun launchChains(initial: R, returnOnFirstError: Boolean = true): Either<E, R> {
        return launchChains(initial = Either.Right(initial), returnOnFirstError)
    }

    private suspend fun launchChains(initial: Either<E, R>, returnOnFirstError: Boolean = true): Either<E, R> {
        var result = initial

        chains.forEach { chain ->
            result = chain(result)
                .onRight {
                    if (returnOnFirstError) return result
                }
        }

        return result
    }
}