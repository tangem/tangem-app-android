package com.tangem.domain.tokens.utils

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class TokenStatusOperations(
    private val token: Token,
    private val quote: Quote?,
    private val networkStatus: NetworkStatus?,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<TokensError>,
) : Raise<TokensError> by raise {

    suspend fun createTokenStatus(): TokenStatus = withContext(dispatchers.single) {
        TokenStatus(
            id = token.id,
            networkId = token.networkId,
            name = token.name,
            symbol = token.symbol,
            isCoin = token.isCoin,
            decimals = token.decimals,
            iconUrl = token.iconUrl,
            value = createStatus(),
        )
    }

    private fun createStatus(): TokenStatus.Status {
        return when (val status = networkStatus?.value) {
            null -> TokenStatus.Loading
            is NetworkStatus.MissedDerivation -> TokenStatus.MissedDerivation
            is NetworkStatus.Unreachable -> TokenStatus.Unreachable
            is NetworkStatus.NoAccount -> TokenStatus.NoAccount
            is NetworkStatus.TransactionInProgress,
            is NetworkStatus.Verified,
            -> createStatus(
                amount = getTokenAmount(),
                hasTransactionsInProgress = status is NetworkStatus.TransactionInProgress,
            )
        }
    }

    private fun createStatus(amount: BigDecimal, hasTransactionsInProgress: Boolean): TokenStatus.Status {
        return when {
            token.isCustom -> TokenStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quote?.fiatRate),
                priceChange = quote?.priceChange,
                hasTransactionsInProgress = hasTransactionsInProgress,
            )
            quote == null -> TokenStatus.Loading
            else -> TokenStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quote.fiatRate),
                priceChange = quote.priceChange,
                hasTransactionsInProgress = hasTransactionsInProgress,
            )
        }
    }

    private fun getTokenAmount(): BigDecimal {
        val amount = networkStatus?.value?.amounts?.get(token.id)
        return ensureNotNull(amount) { TokensError.TokenAmountNotFound(token.id) }
    }

    private fun calculateFiatAmountOrNull(amount: BigDecimal, fiatRate: BigDecimal?): BigDecimal? {
        if (fiatRate == null) return null

        return calculateFiatAmount(amount, fiatRate)
    }

    private fun calculateFiatAmount(amount: BigDecimal, fiatRate: BigDecimal): BigDecimal {
        val fiatAmount = amount * fiatRate
        ensure(condition = fiatAmount >= BigDecimal.ZERO) { TokensError.TokenFiatAmountLessThenZero(token.id) }

        return fiatAmount
    }
}