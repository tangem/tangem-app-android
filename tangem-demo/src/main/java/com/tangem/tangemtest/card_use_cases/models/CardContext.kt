package com.tangem.tangemtest.card_use_cases.models

import androidx.fragment.app.FragmentActivity
import com.tangem.CardManager
import com.tangem.commands.Card
import com.tangem.tangem_sdk_new.extensions.init

/**
[REDACTED_AUTHOR]
 */
class CardContext {
    lateinit var cardManager: CardManager
        private set

    var card: Card? = null
    var isVerified: Boolean = false

    fun reset() {
        isVerified = false
        card = null
    }

    fun requireCard(): Card = card!!

    fun init(activity: FragmentActivity): CardContext {
        if (::cardManager.isInitialized) return this

        cardManager = CardManager.init(activity)
        return this
    }
}