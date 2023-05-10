package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.domain.models.scan.CardDTO

/**
[REDACTED_AUTHOR]
 */
interface ProductCommandProcessor<T> {
    fun proceed(card: CardDTO, session: CardSession, callback: (result: CompletionResult<T>) -> Unit)
}