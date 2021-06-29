package com.tangem.tap.features.wallet.redux.middlewares

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.tap.domain.topup.TopUpHelper
import com.tangem.tap.domain.topup.TopUpManager
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch


class TopUpMiddleware {
    fun handle(action: WalletAction.TopUpAction) {
        when (action) {
            is WalletAction.TopUpAction.TopUp -> {
                val selectedWalletData = store.state.walletState.getSelectedWalletData()
                val config = store.state.globalState.configManager?.config ?: return
                val addresses = selectedWalletData?.walletAddresses ?: return
                if (addresses.list.isEmpty()) return

                val defaultAddress = addresses.list[0].address
                val currency = selectedWalletData.currency

                if (currency is Currency.Token && currency.blockchain.isTestnet()) {
                    val walletManager = store.state.walletState.getWalletManager(currency.token)
                    if (walletManager !is EthereumWalletManager) return

                    scope.launch {
                        TopUpManager().topUpTestErc20Tokens(
                            walletManager = walletManager, token = currency.token
                        )
                    }
                } else {
                    val url = TopUpHelper.getUrl(
                        currency?.blockchain,
                        selectedWalletData.currencyData.currencySymbol!!,
                        defaultAddress,
                        config.moonPayApiKey,
                        config.moonPayApiSecretKey
                    )

                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setToolbarColor(action.toolbarColor)
                        .build()
                    customTabsIntent.launchUrl(action.context, Uri.parse(url))
                }
            }
        }
    }
}