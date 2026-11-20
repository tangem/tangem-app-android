package com.tangem.domain.tokens.operations

import arrow.core.Option
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.utils.annotations.RemoveWithToggle
import java.math.BigDecimal

/**
 * Factory to create [CryptoCurrencyStatus] from [NetworkStatus], [QuoteStatus] and balance contributions.
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
object CryptoCurrencyStatusFactory {

    private val QuoteStatus?.fiatRate: BigDecimal?
        get() = (this?.value as? QuoteStatus.Data)?.fiatRate

    private val QuoteStatus?.priceChange: BigDecimal?
        get() = (this?.value as? QuoteStatus.Data)?.priceChange

    /**
     * Creates [CryptoCurrencyStatus] from [NetworkStatus], [QuoteStatus] and balance contributions.
     *

     * @param maybeNetworkStatus An optional network status containing blockchain information.
     * @param maybeQuoteStatus An optional quote status containing price information.
     * @param contributions Extra balances collected generically at the seam; the only source of staking and
     * yield-supply balances.
     * @param isYieldContributionEnabled leave [CryptoCurrencyStatus.Value.yieldSupplyStatus] empty: yield then
     * reaches readers only as a [YieldSupplyContribution] in [contributions]
     */
    fun create(
        currency: CryptoCurrency,
        maybeNetworkStatus: Option<NetworkStatus>,
        maybeQuoteStatus: Option<QuoteStatus>,
        contributions: List<BalanceContribution>,
        @RemoveWithToggle("TWI_1948_YIELD_CONTRIBUTION_ENABLED")
        isYieldContributionEnabled: Boolean = false,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = createStatus(
                currency = currency,
                maybeNetworkStatus = maybeNetworkStatus,
                maybeQuoteStatus = maybeQuoteStatus,
                contributions = contributions,
                isYieldPositionsSourceEnabled = isYieldContributionEnabled,
            ),
        )
    }

    private fun createStatus(
        currency: CryptoCurrency,
        maybeNetworkStatus: Option<NetworkStatus>,
        maybeQuoteStatus: Option<QuoteStatus>,
        contributions: List<BalanceContribution>,
        isYieldPositionsSourceEnabled: Boolean,
    ): CryptoCurrencyStatus.Value {
        val quoteStatus = maybeQuoteStatus.getOrNull()

        return when (val status = maybeNetworkStatus.getOrNull()?.value) {
            is NetworkStatus.MissedDerivation -> createMissedDerivation(quoteStatus)
            is NetworkStatus.Unreachable -> createUnreachable(status, quoteStatus)
            is NetworkStatus.NoAccount -> createNoAccount(status, quoteStatus)
            is NetworkStatus.Verified -> createStatus(
                currency = currency,
                status = status,
                quoteStatus = quoteStatus,
                contributions = contributions,
                yieldSupplyStatus = if (isYieldPositionsSourceEnabled) {
                    null
                } else {
                    status.getYieldSupplyStatus(currency.id)
                },
            )
            null -> CryptoCurrencyStatus.Loading
        }
    }

    private fun createMissedDerivation(quoteStatus: QuoteStatus?): CryptoCurrencyStatus.MissedDerivation {
        return CryptoCurrencyStatus.MissedDerivation(
            priceChange = quoteStatus.priceChange,
            fiatRate = quoteStatus.fiatRate,
        )
    }

    private fun createUnreachable(
        status: NetworkStatus.Unreachable,
        quoteStatus: QuoteStatus?,
    ): CryptoCurrencyStatus.Unreachable {
        return CryptoCurrencyStatus.Unreachable(
            priceChange = quoteStatus.priceChange,
            fiatRate = quoteStatus.fiatRate,
            networkAddress = status.address,
        )
    }

    private fun createNoAccount(
        status: NetworkStatus.NoAccount,
        quoteStatus: QuoteStatus?,
    ): CryptoCurrencyStatus.NoAccount {
        return CryptoCurrencyStatus.NoAccount(
            amountToCreateAccount = status.amountToCreateAccount,
            fiatAmount = BigDecimal.ZERO,
            priceChange = quoteStatus.priceChange,
            fiatRate = quoteStatus.fiatRate,
            networkAddress = status.address,
            sources = CryptoCurrencyStatus.Sources(
                networkSource = status.source,
                quoteSource = quoteStatus?.value?.source ?: StatusSource.ACTUAL,
            ),
        )
    }

    private fun createStatus(
        currency: CryptoCurrency,
        status: NetworkStatus.Verified,
        quoteStatus: QuoteStatus?,
        contributions: List<BalanceContribution>,
        yieldSupplyStatus: YieldSupplyStatus?,
    ): CryptoCurrencyStatus.Value {
        val networkAmount = when (val amount = status.amounts[currency.id]) {
            is NetworkStatus.Amount.Loaded -> amount.value
            is NetworkStatus.Amount.NotFound -> {
                return createNoAmount(quoteStatus = quoteStatus)
            }
            null -> {
                return CryptoCurrencyStatus.Loading
            }
        }
        val amount = networkAmount.withSpendableContributions(contributions)

        if (currency is CryptoCurrency.Token && currency.isCustom) {
            return createCustom(
                id = currency.id,
                status = status,
                amount = amount,
                quoteStatus = quoteStatus,
                contributions = contributions,
                yieldSupplyStatus = yieldSupplyStatus,
            )
        }

        // order is important for correct total balance calculation
        return when (val quoteValue = quoteStatus?.value) {
            is QuoteStatus.Empty -> {
                createNoQuote(
                    id = currency.id,
                    status = status,
                    amount = amount,
                    quoteStatus = quoteStatus,
                    contributions = contributions,
                    yieldSupplyStatus = yieldSupplyStatus,
                )
            }
            is QuoteStatus.Data -> {
                createLoaded(
                    id = currency.id,
                    status = status,
                    amount = amount,
                    quoteStatus = quoteValue,
                    contributions = contributions,
                    yieldSupplyStatus = yieldSupplyStatus,
                )
            }
            null -> CryptoCurrencyStatus.Loading
        }
    }

    private fun createNoAmount(quoteStatus: QuoteStatus?): CryptoCurrencyStatus.NoAmount {
        return CryptoCurrencyStatus.NoAmount(
            priceChange = quoteStatus.priceChange,
            fiatRate = quoteStatus.fiatRate,
        )
    }

    private fun createCustom(
        id: CryptoCurrency.ID,
        status: NetworkStatus.Verified,
        amount: BigDecimal,
        quoteStatus: QuoteStatus?,
        contributions: List<BalanceContribution>,
        yieldSupplyStatus: YieldSupplyStatus?,
    ): CryptoCurrencyStatus.Custom {
        return CryptoCurrencyStatus.Custom(
            amount = amount,
            fiatAmount = quoteStatus.fiatRate?.let { calculateFiatAmount(amount, it) },
            fiatRate = quoteStatus.fiatRate,
            priceChange = quoteStatus.priceChange,
            hasCurrentNetworkTransactions = status.hasCurrentNetworkTransactions(),
            pendingTransactions = status.getCurrentTransactions(id),
            networkAddress = status.address,
            yieldSupplyStatus = yieldSupplyStatus,
            contributions = contributions,
            sources = CryptoCurrencyStatus.Sources(
                networkSource = status.source,
                contributionSources = contributions.map(BalanceContribution::source),
                quoteSource = quoteStatus?.value?.source ?: StatusSource.ACTUAL,
            ),
        )
    }

    private fun createNoQuote(
        id: CryptoCurrency.ID,
        status: NetworkStatus.Verified,
        amount: BigDecimal,
        quoteStatus: QuoteStatus?,
        contributions: List<BalanceContribution>,
        yieldSupplyStatus: YieldSupplyStatus?,
    ): CryptoCurrencyStatus.NoQuote {
        return CryptoCurrencyStatus.NoQuote(
            amount = amount,
            hasCurrentNetworkTransactions = status.hasCurrentNetworkTransactions(),
            pendingTransactions = status.getCurrentTransactions(id),
            networkAddress = status.address,
            yieldSupplyStatus = yieldSupplyStatus,
            contributions = contributions,
            sources = CryptoCurrencyStatus.Sources(
                networkSource = status.source,
                contributionSources = contributions.map(BalanceContribution::source),
                quoteSource = quoteStatus?.value?.source ?: StatusSource.ACTUAL,
            ),
        )
    }

    private fun createLoaded(
        id: CryptoCurrency.ID,
        status: NetworkStatus.Verified,
        amount: BigDecimal,
        quoteStatus: QuoteStatus.Data,
        contributions: List<BalanceContribution>,
        yieldSupplyStatus: YieldSupplyStatus?,
    ): CryptoCurrencyStatus.Loaded {
        return CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = calculateFiatAmount(amount, quoteStatus.fiatRate),
            fiatRate = quoteStatus.fiatRate,
            priceChange = quoteStatus.priceChange,
            hasCurrentNetworkTransactions = status.hasCurrentNetworkTransactions(),
            pendingTransactions = status.getCurrentTransactions(id),
            networkAddress = status.address,
            yieldSupplyStatus = yieldSupplyStatus,
            contributions = contributions,
            sources = CryptoCurrencyStatus.Sources(
                networkSource = status.source,
                contributionSources = contributions.map(BalanceContribution::source),
                quoteSource = quoteStatus.source,
            ),
        )
    }

    private fun NetworkStatus.Verified.hasCurrentNetworkTransactions() = pendingTransactions.isNotEmpty()

    private fun NetworkStatus.Verified.getCurrentTransactions(id: CryptoCurrency.ID): Set<TxInfo> {
        return pendingTransactions.getOrElse(key = id, defaultValue = ::emptySet)
    }

    @RemoveWithToggle("TWI_1948_YIELD_CONTRIBUTION_ENABLED")
    private fun NetworkStatus.Verified.getYieldSupplyStatus(id: CryptoCurrency.ID): YieldSupplyStatus? {
        return yieldSupplyStatuses[id]
    }

    private fun calculateFiatAmount(amount: BigDecimal, fiatRate: BigDecimal): BigDecimal {
        return amount * fiatRate
    }

    /**
     * Folds what the contributions add to the spendable balance into the network amount, so `amount` and
     * `fiatAmount` keep meaning "what can be sent" wherever that balance is fetched from. Returns the network amount
     * untouched, scale included, when nothing spendable is added.
     */
    private fun BigDecimal.withSpendableContributions(contributions: List<BalanceContribution>): BigDecimal {
        val spendableDelta = contributions.sumOf(BalanceContribution::spendableDeltaCryptoAmount)

        return if (spendableDelta.signum() == 0) this else this + spendableDelta
    }
}