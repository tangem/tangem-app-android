package com.tangem.tap.features.wallet.redux.middlewares

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.tangem.tap.domain.TopUpHelper
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store

class TopUpMiddleware {
    fun handle(action: WalletAction.TopUpAction) {
        when (action) {
            is WalletAction.TopUpAction.TopUp -> {
                val selectedWalletData = store.state.walletState.getSelectedWalletData()
                val config = store.state.globalState.configManager?.config ?: return
                val defaultAddress = selectedWalletData?.walletAddresses!!.list[0].address
                val url = TopUpHelper.getUrl(
                        selectedWalletData.currencyData.currencySymbol!!,
                        defaultAddress,
                        config.moonPayApiKey,
                        config.moonPayApiSecretKey
                )
                val customTabsIntent = CustomTabsIntent.Builder()
                        .setToolbarColor(action.toolbarColor)
                        .build()
                customTabsIntent.launchUrl(action.context, Uri.parse(url));
            }
        }
    }
}