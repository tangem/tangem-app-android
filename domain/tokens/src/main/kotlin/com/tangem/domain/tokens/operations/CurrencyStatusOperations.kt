package com.tangem.domain.tokens.operations

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.*
import java.math.BigDecimal

internal class CurrencyStatusOperations(
    private val currency: CryptoCurrency,
    private val quote: Quote?,
    private val networkStatus: NetworkStatus?,
    private val yieldBalance: YieldBalance?,
    private val ignoreQuote: Boolean,
) {

    private val Quote?.fiatRate: BigDecimal?
        get() = when (this) {
            is Quote.Value -> this.fiatRate
            is Quote.Empty, null -> null
        }

    private val Quote?.priceChange: BigDecimal?
        get() = when (this) {
            is Quote.Value -> this.priceChange
            is Quote.Empty, null -> null
        }

    fun createTokenStatus(): CryptoCurrencyStatus = CryptoCurrencyStatus(currency, createStatus())

    private fun createStatus(): CryptoCurrencyStatus.Value {
        return when (val status = networkStatus?.value) {
            null -> CryptoCurrencyStatus.Loading
            is NetworkStatus.MissedDerivation -> createMissedDerivationStatus()
            is NetworkStatus.Unreachable -> createUnreachableStatus(status)
            is NetworkStatus.NoAccount -> createNoAccountStatus(status)
            is NetworkStatus.Verified -> createStatus(status, yieldBalance)
        }
    }

    private fun createMissedDerivationStatus(): CryptoCurrencyStatus.MissedDerivation =
        CryptoCurrencyStatus.MissedDerivation(priceChange = quote?.priceChange, fiatRate = quote?.fiatRate)

    private fun createUnreachableStatus(status: NetworkStatus.Unreachable): CryptoCurrencyStatus.Unreachable {
        return CryptoCurrencyStatus.Unreachable(
            priceChange = quote?.priceChange,
            fiatRate = quote?.fiatRate,
            networkAddress = status.address,
        )
    }

    private fun createNoAccountStatus(status: NetworkStatus.NoAccount): CryptoCurrencyStatus.NoAccount {
        return CryptoCurrencyStatus.NoAccount(
            amountToCreateAccount = status.amountToCreateAccount,
            fiatAmount = if (quote == null) null else BigDecimal.ZERO,
            priceChange = quote?.priceChange,
            fiatRate = quote?.fiatRate,
            networkAddress = status.address,
            source = listOf(
                status.source,
                (quote as? Quote.Value)?.source ?: StatusSource.ACTUAL,
            ).getResultStatusSource(),
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createStatus(
        networkStatusValue: NetworkStatus.Verified,
        yieldBalance: YieldBalance?,
    ): CryptoCurrencyStatus.Value {
        val amount = when (val amount = networkStatusValue.amounts[currency.id]) {
            null -> {
                return CryptoCurrencyStatus.Loading
            }
            is CryptoCurrencyAmountStatus.NotFound -> {
                return CryptoCurrencyStatus.NoAmount(priceChange = quote?.priceChange, fiatRate = quote?.fiatRate)
            }
            is CryptoCurrencyAmountStatus.Loaded -> amount.value
        }

        val hasCurrentNetworkTransactions = networkStatusValue.pendingTransactions.isNotEmpty()
        val currentTransactions = networkStatusValue.pendingTransactions.getOrElse(currency.id, ::emptySet)
        val yieldBalanceData = yieldBalance as? YieldBalance.Data
        val isCurrentAddressStaking = yieldBalanceData?.address == networkStatusValue.address.defaultAddress.value
        val filteredTokenBalances = yieldBalanceData?.balance?.items?.filter {
            it.token.coinGeckoId == currency.id.rawCurrencyId?.value
        }
        val currentYieldBalance = if (isCurrentAddressStaking && filteredTokenBalances?.isNotEmpty() == true) {
            yieldBalanceData.copy(
                balance = yieldBalanceData.balance.copy(
                    items = filteredTokenBalances,
                ),
            )
        } else {
            null
        }
        // order is important for correct total balance calculation
        return when {
            currency is CryptoCurrency.Token && currency.isCustom -> CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quote?.fiatRate),
                fiatRate = quote?.fiatRate,
                priceChange = quote?.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = networkStatusValue.address,
                yieldBalance = currentYieldBalance,
                source = listOfNotNull(
                    networkStatusValue.source,
                    currentYieldBalance?.source ?: StatusSource.ACTUAL,
                    (quote as? Quote.Value)?.source ?: StatusSource.ACTUAL,
                ).getResultStatusSource(),
            )
            quote is Quote.Empty || ignoreQuote -> CryptoCurrencyStatus.NoQuote(
                amount = amount,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = networkStatusValue.address,
                yieldBalance = currentYieldBalance,
            )
            quote is Quote.Value -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quote.fiatRate),
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = networkStatusValue.address,
                yieldBalance = currentYieldBalance,
                source = listOf(
                    networkStatusValue.source,
                    currentYieldBalance?.source ?: StatusSource.ACTUAL,
                    quote.source,
                ).getResultStatusSource(),
            )
            else -> CryptoCurrencyStatus.Loading
        }
    }

    private fun calculateFiatAmountOrNull(amount: BigDecimal, fiatRate: BigDecimal?): BigDecimal? {
        if (fiatRate == null) return null

        return calculateFiatAmount(amount, fiatRate)
    }

    private fun calculateFiatAmount(amount: BigDecimal, fiatRate: BigDecimal): BigDecimal {
        return amount * fiatRate
    }
}