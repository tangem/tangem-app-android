package com.tangem.domain.tokens.operations

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import java.math.BigDecimal

internal class CurrencyStatusOperations(
    private val currency: CryptoCurrency,
    private val quoteStatus: QuoteStatus?,
    private val networkStatus: NetworkStatus?,
    private val yieldBalance: YieldBalance?,
    private val ignoreQuote: Boolean,
) {

    private val QuoteStatus?.fiatRate: BigDecimal?
        get() = (this?.value as? QuoteStatus.Data)?.fiatRate

    private val QuoteStatus?.priceChange: BigDecimal?
        get() = (this?.value as? QuoteStatus.Data)?.priceChange

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
        CryptoCurrencyStatus.MissedDerivation(priceChange = quoteStatus?.priceChange, fiatRate = quoteStatus?.fiatRate)

    private fun createUnreachableStatus(status: NetworkStatus.Unreachable): CryptoCurrencyStatus.Unreachable {
        return CryptoCurrencyStatus.Unreachable(
            priceChange = quoteStatus?.priceChange,
            fiatRate = quoteStatus?.fiatRate,
            networkAddress = status.address,
        )
    }

    private fun createNoAccountStatus(status: NetworkStatus.NoAccount): CryptoCurrencyStatus.NoAccount {
        return CryptoCurrencyStatus.NoAccount(
            amountToCreateAccount = status.amountToCreateAccount,
            fiatAmount = if (quoteStatus == null) null else BigDecimal.ZERO,
            priceChange = quoteStatus?.priceChange,
            fiatRate = quoteStatus?.fiatRate,
            networkAddress = status.address,
            sources = CryptoCurrencyStatus.Sources(
                networkSource = status.source,
                quoteSource = quoteStatus?.value?.source ?: StatusSource.ACTUAL,
            ),
        )
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun createStatus(
        networkStatusValue: NetworkStatus.Verified,
        yieldBalance: YieldBalance?,
    ): CryptoCurrencyStatus.Value {
        val amount = when (val amount = networkStatusValue.amounts[currency.id]) {
            null -> {
                return CryptoCurrencyStatus.Loading
            }
            is NetworkStatus.Amount.NotFound -> {
                return CryptoCurrencyStatus.NoAmount(
                    priceChange = quoteStatus?.priceChange,
                    fiatRate = quoteStatus?.fiatRate,
                )
            }
            is NetworkStatus.Amount.Loaded -> amount.value
        }

        val hasCurrentNetworkTransactions = networkStatusValue.pendingTransactions.isNotEmpty()
        val currentTransactions = networkStatusValue.pendingTransactions.getOrElse(currency.id, ::emptySet)
        val yieldBalanceData = yieldBalance as? YieldBalance.Data
        val isCurrentAddressStaking =
            yieldBalanceData?.stakingId?.address == networkStatusValue.address.defaultAddress.value
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

        val quoteValue = quoteStatus?.value

        // order is important for correct total balance calculation
        return when {
            currency is CryptoCurrency.Token && currency.isCustom -> CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = calculateFiatAmountOrNull(amount, quoteStatus?.fiatRate),
                fiatRate = quoteStatus?.fiatRate,
                priceChange = quoteStatus?.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = networkStatusValue.address,
                yieldBalance = currentYieldBalance,
                sources = CryptoCurrencyStatus.Sources(
                    networkSource = networkStatusValue.source,
                    quoteSource = quoteStatus?.value?.source ?: StatusSource.ACTUAL,
                    yieldBalanceSource = currentYieldBalance?.source ?: StatusSource.ACTUAL,
                ),
            )
            quoteValue is QuoteStatus.Empty || ignoreQuote -> CryptoCurrencyStatus.NoQuote(
                amount = amount,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = networkStatusValue.address,
                yieldBalance = currentYieldBalance,
                sources = CryptoCurrencyStatus.Sources(
                    networkSource = networkStatusValue.source,
                    quoteSource = quoteStatus?.value?.source ?: StatusSource.ACTUAL,
                    yieldBalanceSource = currentYieldBalance?.source ?: StatusSource.ACTUAL,
                ),
            )
            quoteValue is QuoteStatus.Data -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = calculateFiatAmount(amount, quoteValue.fiatRate),
                fiatRate = quoteValue.fiatRate,
                priceChange = quoteValue.priceChange,
                hasCurrentNetworkTransactions = hasCurrentNetworkTransactions,
                pendingTransactions = currentTransactions,
                networkAddress = networkStatusValue.address,
                yieldBalance = currentYieldBalance,
                sources = CryptoCurrencyStatus.Sources(
                    networkSource = networkStatusValue.source,
                    quoteSource = quoteValue.source,
                    yieldBalanceSource = currentYieldBalance?.source ?: StatusSource.ACTUAL,
                ),
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