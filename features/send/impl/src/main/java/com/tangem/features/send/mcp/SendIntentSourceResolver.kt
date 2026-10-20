package com.tangem.features.send.mcp

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.lib.crypto.BlockchainUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Finds the account and the currency of a wallet a send intent sends from. The intent names no wallet or account, so
 * its sender address decides: only the wallet's own accounts count, and the one whose address of the intent's token
 * equals that address is the source.
 */
internal class SendIntentSourceResolver @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    /** Returns `null` when the accounts of the wallet cannot be read. */
    suspend fun resolve(userWalletId: UserWalletId, intent: SendIntent): SendIntentSource? {
        val portfolios = loadPortfolios(userWalletId, intent) ?: return null

        return portfolios.firstNotNullOfOrNull { portfolio ->
            portfolio.flattenCurrencies()
                .firstOrNull { status -> intent.matches(status.currency) && intent.isSentFrom(status) }
                ?.let { status -> SendIntentSource.Found(accountId = portfolio.accountId, status = status) }
        } ?: SendIntentSource.NotFound
    }

    /**
     * A `Loading` status carries no address yet, so an intent could be found in no account just because its token has
     * not loaded. Waits a bounded time for the statuses of the intent's token to settle (the rest of the wallet does
     * not affect the result) and keeps the latest emission seen, so a flow that never settles still resolves and one
     * that never emits gives `null`.
     */
    private suspend fun loadPortfolios(userWalletId: UserWalletId, intent: SendIntent): List<AccountStatus.Personal>? {
        var latest: AccountStatusList? = null

        withTimeoutOrNull(STATUSES_TIMEOUT_MILLIS) {
            singleAccountStatusListSupplier(userWalletId).firstOrNull { list ->
                latest = list
                list.personalAccounts().none { it.hasLoadingStatusOf(intent) }
            }
        }

        return latest?.personalAccounts()
    }

    private fun AccountStatusList.personalAccounts() = accountStatuses.filterIsInstance<AccountStatus.Personal>()

    private fun AccountStatus.Personal.hasLoadingStatusOf(intent: SendIntent) = flattenCurrencies().any { status ->
        status.value is CryptoCurrencyStatus.Loading && intent.matches(status.currency)
    }

    private fun SendIntent.matches(currency: CryptoCurrency): Boolean {
        return currency.network.rawId == networkId && currency.id.rawCurrencyId?.value == tokenId
    }

    private fun SendIntent.isSentFrom(status: CryptoCurrencyStatus): Boolean {
        val networkAddress = status.value.networkAddress ?: return false
        val addresses = networkAddress.availableAddresses + networkAddress.defaultAddress
        // An EVM address reaches the app checksummed or lowercased; elsewhere (base58, bech32) a case variant is
        // another address
        val isCaseInsensitive = BlockchainUtils.isCaseInsensitiveContractAddress(status.currency.network.rawId)
        return addresses.any { it.value.equals(fromAddress, ignoreCase = isCaseInsensitive) }
    }

    private companion object {
        const val STATUSES_TIMEOUT_MILLIS = 3_000L
    }
}