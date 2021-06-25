package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.features.tokens.ui.adapters.CurrencyListItem
import org.rekotlin.StateType

data class TokensState(
        val addedTokens: LinkedHashSet<TokenWithAmount> = LinkedHashSet(),
        val addedCurrencies: CardCurrencies? = null,
        val currencies: List<CurrencyListItem> = emptyList(),
) : StateType

data class TokenWithAmount(
        val token: Token,
        val amount: Amount? = null
)

