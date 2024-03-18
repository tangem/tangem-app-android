package com.tangem.features.send.impl.navigation

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.features.send.impl.presentation.SendFragment
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter

internal class DefaultSendRouter(
    private val reduxNavController: ReduxNavController,
) : InnerSendRouter {

    override fun getEntryFragment(): Fragment = SendFragment.create()

    override fun openUrl(url: String) {
        reduxNavController.navigate(NavigationAction.OpenUrl(url = url))
    }

    override fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
        reduxNavController.popBackStack()
        reduxNavController.navigate(
            action = NavigationAction.NavigateTo(
                screen = AppScreen.WalletDetails,
                bundle = bundleOf(
                    TokenDetailsRouter.USER_WALLET_ID_KEY to userWalletId.stringValue,
                    TokenDetailsRouter.CRYPTO_CURRENCY_KEY to currency,
                ),
            ),
        )
    }

    override fun openQrCodeScanner(network: String) {
        reduxNavController.navigate(
            action = NavigationAction.NavigateTo(
                screen = AppScreen.QrScanning,
                bundle = bundleOf(
                    QrScanningRouter.SOURCE_KEY to SourceType.SEND,
                    QrScanningRouter.NETWORK_KEY to network,
                ),
            ),
        )
    }
}
