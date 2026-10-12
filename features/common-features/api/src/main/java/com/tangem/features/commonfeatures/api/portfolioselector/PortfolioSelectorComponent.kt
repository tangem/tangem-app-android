package com.tangem.features.commonfeatures.api.portfolioselector

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * How to use
 * 1) Create and keep instance of [PortfolioFetcher] and [PortfolioSelectorController] in your Feature Model
 * 2) Provide them via [Params]
 * 3) Now you have a bridge between your Feature and PortfolioSelector
 * 4) Initial state is unselected. Select yourself [PortfolioSelectorController.selectAccount]
 * or offer users to select
 *
 * 5) Listen [PortfolioSelectorController.selectedAccount] or [PortfolioSelectorController.selectedAccountWithData]
 *
 * Note:
 *  - Supports [PortfolioSelectorComponent.BottomSheet] and [PortfolioSelectorComponent.Content] modes
 */
interface PortfolioSelectorComponent : ComposableBottomSheetComponent, ComposableContentComponent {

    val title: StateFlow<TextReference>

    data class Params(
        val portfolioFetcher: PortfolioFetcher,
        val controller: PortfolioSelectorController,
        val bsCallback: BottomSheetCallback? = null,
        val settings: Settings = Settings(),
    )

    /**
     * @property isWalletSelectionOnly when `true`, the selector always shows a flat wallet list and ignores the
     * global accounts mode (no account grouping).
     * @property isJointAccountsIncluded whether active joint accounts are offered next to the wallet's own ones; a joint
     * account holds funds in a Safe, so only the flows that can work with it (adding tokens) opt in
     * @property isDs3Modal shows the bottom sheet as the translucent DS3 modal. Its content is not lazy, so opt in
     * only where the list stays short
     */
    data class Settings(
        val isWalletSelectionOnly: Boolean = false,
        val isMultiChoice: Boolean = false,
        val isJointAccountsIncluded: Boolean = false,
        val isDs3Modal: Boolean = false,
    )

    interface BottomSheetCallback {
        val onDismiss: () -> Unit
        val onBack: () -> Unit
    }

    interface Factory : ComponentFactory<Params, PortfolioSelectorComponent>
}

/**
 * How to use see [PortfolioSelectorComponent]
 *
 * if [isAccountMode] is false it's mean [selectedAccount] emit [AccountId] for Main account
 */
interface PortfolioSelectorController {
    val isAccountMode: Flow<Boolean>

    val selectedAccounts: Flow<Set<AccountId>>
    val selectedAccount: Flow<AccountId?> get() = selectedAccounts.map { it.firstOrNull() }

    val selectedAccountsSync: Set<AccountId>
    val selectedAccountSync: AccountId? get() = selectedAccountsSync.firstOrNull()

    /**
     * for some Feature specific filtering
     * combine and update with your Feature data and [PortfolioFetcher.data]
     */
    val isEnabled: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>
    suspend fun isAccountModeSync(): Boolean

    fun selectAccount(accountIds: Set<AccountId>)
    fun selectAccount(accountId: AccountId?) = selectAccount(accountId?.let { setOf(it) }.orEmpty())

    fun selectedAccountsWithData(
        portfolioFetcher: PortfolioFetcher,
    ): Flow<Set<Pair<UserWallet, AccountStatus.CryptoPortfolio>>>

    fun selectedAccountWithData(
        portfolioFetcher: PortfolioFetcher,
    ): Flow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?> =
        selectedAccountsWithData(portfolioFetcher).map { it.firstOrNull() }
}