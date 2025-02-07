package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
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

    val dialogNavigation: SlotNavigation<WalletDialogConfig>

    fun initializeResources(appComponentContext: AppComponentContext)

    /**
     * Initialize router
     *
     * @param onFinish finish activity callback
     */
    @Composable
    fun Initialize(onFinish: () -> Unit)

    /** Pop back stack */
    fun popBackStack()

    /** Open organize tokens screen */
    fun openOrganizeTokensScreen(userWalletId: UserWalletId)

    /** Open details screen */
    fun openDetailsScreen(selectedWalletId: UserWalletId)

    /** Open onboarding screen */
    fun openOnboardingScreen(scanResponse: ScanResponse, continueBackup: Boolean = false)

    /** Open onramp success screen for [externalTxId] */
    fun openOnrampSuccessScreen(externalTxId: String)

    /** Open transaction history website by [url] */
    fun openUrl(url: String)

    /** Open token details screen */
    fun openTokenDetails(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus)

    /** Open stories screen */
    fun openStoriesScreen()

    /** Open save user wallet screen */
    fun openSaveUserWalletScreen()

    /** Is wallet last screen */
    fun isWalletLastScreen(): Boolean

    /** Open manage tokens screen */
    fun openManageTokensScreen(userWalletId: UserWalletId)

    /** Open scan failed dialog */
    fun openScanFailedDialog(onTryAgain: () -> Unit)
}