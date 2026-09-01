package com.tangem.data.account.converter

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet

/**
 * The token mapping shared by the account converters: a crypto portfolio row and a joint one carry their tokens
 * identically, and only the account they belong to differs.
 */

/** The tokens of a `GET /accounts` row as domain currencies; nothing to convert yields an empty list. */
internal fun ResponseCryptoCurrenciesFactory.createAccountCurrencies(
    tokens: List<UserTokensResponse.Token>?,
    userWallet: UserWallet,
    accountIndex: DerivationIndex,
): List<CryptoCurrency> {
    if (tokens.isNullOrEmpty()) return emptyList()

    return createCurrencies(tokens = tokens, userWallet = userWallet, accountIndex = accountIndex)
}

/** The currencies of an account as the tokens of its `GET /accounts` row. */
internal fun UserTokensResponseFactory.createAccountTokens(
    currencies: List<CryptoCurrency>,
    accountId: AccountId,
): List<UserTokensResponse.Token> {
    return currencies.map { currency -> createResponseToken(currency = currency, accountId = accountId) }
}