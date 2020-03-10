package com.tangem.tangemtest

import android.app.Application
import com.tangem.tangemtest.card_use_cases.CardContext
import com.tangem.tangemtest.commons.DiManager

/**
[REDACTED_AUTHOR]
 */
class AppTangemDemo : Application(), DiManager {
    private val cardContext: CardContext = CardContext()

    override fun getCardContext(): CardContext = cardContext
}