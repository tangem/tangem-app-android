package com.tangem.tap.domain.walletStores.implementation.utils

import com.tangem.common.CompletionResult

internal fun List<CompletionResult<Unit>>.fold(): CompletionResult<Unit> {
    return fold(Unit) { _, _ -> Unit }
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified D, reified R> List<CompletionResult<D>>.fold(
    initial: R,
    operation: (acc: R, data: D) -> R,
): CompletionResult<R> {
    var resultData = initial
    for (result in this) {
        when (result) {
            is CompletionResult.Success -> {
                resultData = operation(resultData, result.data)
            }
            is CompletionResult.Failure -> {
                return result as CompletionResult.Failure<R>
            }
        }
    }

    return CompletionResult.Success(resultData)
}
