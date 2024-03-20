package com.tangem.feature.tokendetails.presentation.router

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.tokendetails.presentation.TokenDetailsFragment
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter

internal class DefaultTokenDetailsRouter(
    private val reduxNavController: ReduxNavController,
) : InnerTokenDetailsRouter {

    override fun getEntryFragment(): Fragment = TokenDetailsFragment()

    override fun popBackStack() {
        reduxNavController.navigate(NavigationAction.PopBackTo())
    }

    override fun openUrl(url: String) {
        reduxNavController.navigate(NavigationAction.OpenUrl(url = url))
    }

    override fun share(text: String) {
        reduxNavController.navigate(NavigationAction.Share(text))
    }

    override fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
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
}