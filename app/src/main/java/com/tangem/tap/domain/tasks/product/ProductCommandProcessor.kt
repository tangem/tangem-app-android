package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession

/**
[REDACTED_AUTHOR]
 */
interface ProductCommandProcessor<T> {
    fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<T>) -> Unit,
    )
}