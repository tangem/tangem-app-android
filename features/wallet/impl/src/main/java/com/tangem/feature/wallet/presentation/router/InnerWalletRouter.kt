package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tangem.core.navigation.AppScreen
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.wallet.navigation.WalletRouter

/**
 * Interface of inner wallet feature router
 *
 * Annotated as [Stable] because implementation of this interface passed as argument to [Initialize] method by compose
 * compiler
 *
[REDACTED_AUTHOR]
 */
@Stable
internal interface InnerWalletRouter : WalletRouter {

    /**
     * Initialize router
     *
     * @param onFinish finish activity callback
     */
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun Initialize(onFinish: () -> Unit)

    /** Pop back stack */
    fun popBackStack(screen: AppScreen? = null)

    /** Open organize tokens screen */
    fun openOrganizeTokensScreen(userWalletId: UserWalletId)

    /** Open details screen */
    fun openDetailsScreen()

    /** Open onboarding screen */
    fun openOnboardingScreen()

    /** Open transaction history website by [url] */
    fun openUrl(url: String)

    /** Open token details screen */
    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)

    /** Open stories screen */
    fun openStoriesScreen()

    /** Open save user wallet screen */
    fun openSaveUserWalletScreen()

    /** Is wallet last screen */
    fun isWalletLastScreen(): Boolean

    /** Open manage tokens screen */
    fun openManageTokensScreen()

    fun openScanFailedDialog()
}