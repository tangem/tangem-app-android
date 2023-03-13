package com.tangem.domain.core.chain

interface Chain<R> {
    suspend operator fun invoke(previousChainResult: ChainResult<R>): ChainResult<R>
}
