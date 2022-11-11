package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.domain.common.CardDTO

/**
 * Created by Anton Zhilenkov on 28/09/2021.
 */
interface ProductCommandProcessor<T> {
    fun proceed(
        card: CardDTO,
        session: CardSession,
        callback: (result: CompletionResult<T>) -> Unit,
    )
}
