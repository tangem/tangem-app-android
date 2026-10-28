package com.tangem.feature.swap.mcp.holdings

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.lib.crypto.BlockchainUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Finds the currencies of a wallet that an exchange intent's assets stand for, the way the `tangem://swap` deeplink picks its
 * tokens: only the wallet's own accounts count, the asset to swap from comes from the most funded account holding it,
 * and the asset to swap to prefers the account the other one came from.
 */
internal class ExchangeIntentHoldingsResolver @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    /** Returns `null` when the accounts of the wallet cannot be read. */
    suspend fun resolve(userWalletId: UserWalletId, intent: ExchangeIntent): ExchangeIntentHoldings? {
        val portfolios = loadPortfolios(userWalletId, intent) ?: return null

        val from = portfolios.findHeld(asset = intent.from, preferredAccountId = null)
        val to = portfolios.findHeld(asset = intent.to, preferredAccountId = from?.accountId)

        return when {
            from != null && to != null -> ExchangeIntentHoldings.Both(from = from.currency, to = to.currency)
            from != null -> ExchangeIntentHoldings.MissingTo(from = from.currency)
            to != null -> ExchangeIntentHoldings.MissingFrom(to = to.currency)
            else -> ExchangeIntentHoldings.MissingBoth
        }
    }

    /**
     * The first emission may still carry `Loading` balances, which rank as zero, so the most funded account would be
     * picked blindly. Waits a bounded time for the balances of the intent's two assets to settle (the rest of the
     * wallet does not affect the result) and keeps the latest emission seen, so a flow that never settles still
     * resolves and one that never emits gives `null`.
     */
    private suspend fun loadPortfolios(
        userWalletId: UserWalletId,
        intent: ExchangeIntent,
    ): List<AccountStatus.Personal>? {
        var latest: AccountStatusList? = null

        withTimeoutOrNull(BALANCES_TIMEOUT_MILLIS) {
            singleAccountStatusListSupplier(userWalletId).firstOrNull { list ->
                latest = list
                list.personalAccounts().none { it.hasLoadingBalanceOf(intent) }
            }
        }

        return latest?.personalAccounts()
    }

    private fun AccountStatusList.personalAccounts() = accountStatuses.filterIsInstance<AccountStatus.Personal>()

    private fun AccountStatus.Personal.hasLoadingBalanceOf(intent: ExchangeIntent) = flattenCurrencies().any { status ->
        status.value is CryptoCurrencyStatus.Loading &&
            (intent.from.matches(status.currency) || intent.to.matches(status.currency))
    }

    private fun List<AccountStatus.Personal>.findHeld(
        asset: ExchangeIntent.Asset,
        preferredAccountId: AccountId?,
    ): HeldCurrency? {
        val candidates = flatMap { portfolio ->
            portfolio.flattenCurrencies()
                .filter { status -> asset.matches(status.currency) }
                .map { status -> HeldCurrency(accountId = portfolio.accountId, status = status) }
        }
        val pool = candidates.filter { it.accountId == preferredAccountId }.ifEmpty { candidates }

        return pool.maxByOrNull { it.status.value.fiatAmount ?: BigDecimal.ZERO }
    }

    private fun ExchangeIntent.Asset.matches(currency: CryptoCurrency): Boolean {
        val id = ExpressAsset.ID(currency)
        return network.equals(id.networkId, ignoreCase = true) &&
            contractAddress.equals(
                other = id.contractAddress,
                ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(currency.network.rawId),
            )
    }

    private data class HeldCurrency(val accountId: AccountId, val status: CryptoCurrencyStatus) {
        val currency: CryptoCurrency get() = status.currency
    }

    private companion object {
        const val BALANCES_TIMEOUT_MILLIS = 3_000L
    }
}