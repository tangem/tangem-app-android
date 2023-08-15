package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.fragment.app.FragmentManager
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.wallet.navigation.WalletRouter

/**
 * Interface of inner wallet feature router
 *
 * Annotated as [Stable] because implementation of this interface passed as argument to [Initialize] method by compose
 * compiler
 *
* [REDACTED_AUTHOR]
 */
@Stable
internal interface InnerWalletRouter : WalletRouter {

    /**
     * Initialize router
     *
     * @param fragmentManager fragment manager
     */
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun Initialize(fragmentManager: FragmentManager)

    /** Pop back stack */
    fun popBackStack()

    /** Open organize tokens screen */
    fun openOrganizeTokensScreen(userWalletId: UserWalletId)

    /** Open details screen */
    fun openDetailsScreen()

    /** Open onboarding screen */
    fun openOnboardingScreen()

    /** Open transaction history website by [url] */
    fun openTxHistoryWebsite(url: String)

    /** Open token details screen */
    fun openTokenDetails(currency: CryptoCurrency)
}
