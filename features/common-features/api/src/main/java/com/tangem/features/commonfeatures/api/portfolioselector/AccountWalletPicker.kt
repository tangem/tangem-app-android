package com.tangem.features.commonfeatures.api.portfolioselector

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Settles the wallet a new account goes to: the unlocked wallets able to hold accounts, each one's account list
 * and the "Choose wallet" sheet. In the sheet a wallet at the cap stays tappable with a "Full" badge, so the user
 * learns why it can't be used; locked wallets are listed but disabled.
 *
 * Every wallet's account list is read on its own, so one that never loads only leaves that wallet out. Nothing
 * is read before [wallets], [resolve] or [openChooser] is first used, so a flow that already knows its wallet
 * pays nothing for holding a picker.
 */
interface AccountWalletPicker {

    /** Drives the sheet; host it with [selectorParams]. */
    val navigation: SlotNavigation<Unit>

    /** Read only when the sheet is created: building it starts reading every wallet's balances. */
    val selectorParams: PortfolioSelectorComponent.Params

    /** `null` until the wallet list is read. */
    val wallets: StateFlow<List<UserWallet>?>

    val picks: Flow<Pick>

    /**
     * Waits for the account lists (bounded by a timeout) and decides how Continue goes on. A wallet whose list did
     * not load in time is left out.
     */
    suspend fun resolve(hasRoom: (AccountList) -> Boolean): Resolution

    /** Opens the sheet; [fullWalletIds] get the "Full" badge and report [Pick.Full]. */
    fun openChooser(fullWalletIds: Set<UserWalletId>)

    sealed interface Resolution {
        /** No wallet's account list loaded, so the cap is unknown. */
        data object Unavailable : Resolution

        /** The only wallet is at the cap. */
        data object LimitReached : Resolution

        /** The only wallet has room. */
        data class Single(val walletId: UserWalletId) : Resolution

        data class Choose(val fullWalletIds: Set<UserWalletId>) : Resolution
    }

    sealed interface Pick {
        /** The sheet is already dismissed. */
        data class Wallet(val walletId: UserWalletId) : Pick

        /** The sheet stays open under the limit dialog the caller shows. */
        data object Full : Pick
    }

    interface Factory {
        fun create(scope: CoroutineScope): AccountWalletPicker
    }
}