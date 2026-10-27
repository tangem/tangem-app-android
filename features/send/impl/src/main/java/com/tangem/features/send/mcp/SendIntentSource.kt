package com.tangem.features.send.mcp

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus

/** Where in a wallet a send intent sends from. */
internal sealed interface SendIntentSource {

    data class Found(val accountId: AccountId, val status: CryptoCurrencyStatus) : SendIntentSource {
        val currency: CryptoCurrency get() = status.currency
    }

    /** No account of the wallet holds the intent's token. */
    data object TokenMissing : SendIntentSource

    /** The wallet holds the intent's token, but at other addresses: the intent sends from another wallet. */
    data object OtherSender : SendIntentSource
}