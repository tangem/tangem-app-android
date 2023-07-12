package com.tangem.domain.tokens.error

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.Token

sealed class TokensError {

    object EmptyTokens : TokensError()

    object EmptyQuotes : TokensError()

    object EmptyNetworks : TokensError()

    object EmptyNetworkStatues : TokensError()

    data class TokenAmountNotFound(val tokenId: Token.ID) : TokensError()

    data class TokenFiatAmountLessThenZero(val tokenId: Token.ID) : TokensError()

    data class NetworkNotFound(val networkId: Network.ID) : TokensError()
}