package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.tokens.model.details.TokenAction
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

    /** Open transaction history website by [url] */
    fun openUrl(url: String)

    /** Open token details screen */
    fun openTokenDetails(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        navigationAction: NavigationAction? = null,
    )

    /** Open stories screen */
    fun openStoriesScreen()

    /** Is wallet last screen */
    fun isWalletLastScreen(): Boolean

    /** Open scan failed dialog */
    fun openScanFailedDialog(onTryAgain: () -> Unit)

    /** Open NFT collections screen */
    fun openNFT(userWallet: UserWallet)

    fun openTokenReceiveBottomSheet(tokenReceiveConfig: TokenReceiveConfig)

    fun openTangemPayOnboarding(mode: AppRoute.TangemPayOnboarding.Mode)

    fun openTangemPayDetails(userWalletId: UserWalletId, config: TangemPayDetailsConfig)

    /** Open BS abput yield supply active and all money deposited in AAVE */
    fun openYieldSupplyBottomSheet(
        cryptoCurrency: CryptoCurrency,
        tokenAction: TokenAction,
        onWarningAcknowledged: (TokenAction) -> Unit,
    )

    /** Open yield supply entry screen */
    fun openYieldSupplyEntryScreen(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, apy: String)
}