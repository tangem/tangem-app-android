package com.tangem.tap.common.pushes

import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.tap.ForegroundActivityObserver
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles a received token-details push (same payload as
 * [com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler]).
 *
 * When the app is open and the pushed token is not yet present in the wallet's portfolio (e.g. it was just added
 * on the backend), refreshes the wallet accounts so it appears locally — the open portfolio screen then updates
 * reactively via [SingleAccountListSupplier]. Does nothing else.
 */
class TokenDetailsPushHandler @Inject constructor(
    private val appCoroutineScope: AppCoroutineScope,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val singleAccountListFetcher: SingleAccountListFetcher,
) {

    fun handle(queryParams: Map<String, String>) {
        // Only when the app is open: a token just added on the backend should appear in the already-open portfolio.
        // On cold start the fresh list is loaded by the regular auth flow instead.
        if (ForegroundActivityObserver.foregroundActivity == null) return
        appCoroutineScope.launch { refreshPortfolioIfTokenMissing(queryParams) }
    }

    internal suspend fun refreshPortfolioIfTokenMissing(queryParams: Map<String, String>) {
        val networkId = queryParams[NETWORK_ID_KEY] ?: return
        val tokenId = queryParams[TOKEN_ID_KEY] ?: return
        val derivationPath = queryParams[DERIVATION_PATH_KEY]

        val userWallet = resolveUserWallet(queryParams[WALLET_ID_KEY]) ?: return
        // Token list refresh only makes sense for an unlocked multi-currency wallet.
        if (userWallet.isLocked || !userWallet.isMultiCurrency) return

        val isTokenPresent = singleAccountListSupplier.getSyncOrNull(userWallet.walletId)
            ?.flattenCurrencies()
            ?.any { it.matches(networkId = networkId, tokenId = tokenId, derivationPath = derivationPath) } == true

        if (isTokenPresent) return

        singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWallet.walletId))
            .onLeft { TangemLogger.e("Error on refreshing portfolio from push", it) }
    }

    private fun resolveUserWallet(walletId: String?): UserWallet? {
        val userWalletId = walletId?.let(::UserWalletId)
        return if (userWalletId != null) {
            getUserWalletUseCase(userWalletId).getOrNull()
        } else {
            getSelectedWalletSyncUseCase().getOrNull()
        }
    }

    private fun CryptoCurrency.matches(networkId: String, tokenId: String, derivationPath: String?): Boolean {
        val isNetwork = network.rawId.equals(networkId, ignoreCase = true)
        val isCurrency = id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true
        val isDefaultDerivation = network.derivationPath is Network.DerivationPath.Card
        val isCustomDerivation = derivationPath?.equals(network.derivationPath.value) == true
        return isNetwork && isCurrency && (isDefaultDerivation || isCustomDerivation)
    }
}