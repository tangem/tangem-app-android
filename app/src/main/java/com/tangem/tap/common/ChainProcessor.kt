package com.tangem.tap.common

/**
 * Created by Anton Zhilenkov on 05.01.2023.
 */
interface ChainProcessor<T> {
    fun addChain(chain: Chain<T>)
    suspend fun launch(): T
}

interface Chain<T> {
    suspend fun invoke(data: T): T
}

sealed class ChainResult<out T> {
    data class Success<out T>(val data: T) : ChainResult<T>()
    data class Failure(val error: Throwable) : ChainResult<Nothing>()
}

inline fun <T> ChainResult<T>.successOr(failureClause: (ChainResult.Failure) -> T): T {
    return when (this) {
        is ChainResult.Success -> this.data
        is ChainResult.Failure -> failureClause(this)
    }
}

sealed class ChainError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Throwable()
