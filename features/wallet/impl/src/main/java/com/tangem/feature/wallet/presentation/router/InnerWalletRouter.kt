package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface of inner wallet feature router
 *
 * Annotated as [Stable] because implementation of this interface passed as argument to [Initialize] method by compose
 * compiler
 *
[REDACTED_AUTHOR]
 */
@Stable
internal interface InnerWalletRouter {

    val dialogNavigation: SlotNavigation<WalletDialogConfig>

    val navigateToFlow: SharedFlow<WalletRoute>

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

    /** Open NFT collections screen */
    fun openNFT(userWalletId: UserWalletId)
}