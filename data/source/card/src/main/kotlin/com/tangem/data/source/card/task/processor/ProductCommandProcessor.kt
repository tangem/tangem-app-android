package com.tangem.data.source.card.task.processor

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession

/**
 * Created by Anton Zhilenkov on 28/09/2021.
 */
internal interface ProductCommandProcessor<T> {
    fun proceed(card: Card, session: CardSession, callback: (result: CompletionResult<T>) -> Unit)
}
