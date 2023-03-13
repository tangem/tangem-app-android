package com.tangem.domain.core.chain

abstract class ChainProcessor<R> {
    private val chains: MutableList<Chain<R>> = mutableListOf()

    protected fun addChain(chain: Chain<R>) {
        chains.add(chain)
    }

    protected suspend fun launchChains(returnOnFirstError: Boolean = true): ChainResult<R> {
        var result: ChainResult<R> = ChainResult.Initial

        for (chain in chains) {
            result = chain(result)
            when (result) {
                is ChainResult.Initial,
                is ChainResult.Failure,
                -> {
                    if (returnOnFirstError) break
                }
                is ChainResult.Success -> continue
            }
        }

        return result
    }
}
