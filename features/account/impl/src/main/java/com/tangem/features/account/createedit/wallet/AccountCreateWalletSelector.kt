package com.tangem.features.account.createedit.wallet

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**

 *
 * Each wallet's account list is read on its own: one wallet that never loads must not hide the row, it only
 * stays disabled in the chooser, like the wallets already at the account cap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountCreateWalletSelector(
    initialWalletId: UserWalletId,
    private val scope: CoroutineScope,
    userWalletsListRepository: UserWalletsListRepository,
    singleAccountListSupplier: SingleAccountListSupplier,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
) {

    val navigation = SlotNavigation<Unit>()

    /** Lazy: reading every wallet's balances is only worth it once the chooser is actually opened. */
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
            scope = scope,
        )
    }

    val bottomSheetCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { navigation.dismiss() }
        override val onBack: () -> Unit = { navigation.dismiss() }
    }

    val selectedWalletId: StateFlow<UserWalletId>
        field = MutableStateFlow(initialWalletId)

    private val accountCapableWallets: StateFlow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filter { it.isAccountsSupported && !it.isLocked } }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private val accountLists: StateFlow<Map<UserWalletId, AccountList>> = accountCapableWallets
        .map { wallets -> wallets.map(UserWallet::walletId) }
        .distinctUntilChanged()
        .flatMapLatest { walletIds ->
            walletIds.map(singleAccountListSupplier::invoke)
                .merge()
                .scan(emptyMap<UserWalletId, AccountList>()) { lists, list -> lists + (list.userWalletId to list) }
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyMap())

    /** A single wallet leaves nothing to choose, so the row is not shown at all */
    val walletRow: Flow<AccountCreateEditUM.Wallet?> = combine(
        accountCapableWallets,
        selectedWalletId,
    ) { wallets, selectedId ->
        val selected = wallets.firstOrNull { it.walletId == selectedId }
        if (wallets.size > 1 && selected != null) {
            AccountCreateEditUM.Wallet(name = selected.name, onClick = ::openChooser)
        } else {
            null
        }
    }

    init {
        observePickedWallet()
    }

    private fun openChooser() {
        val eligible = accountLists.value.values
            .filter { it.canAddMoreCryptoAccounts }
            .mapTo(mutableSetOf()) { it.userWalletId }
        // The chooser lists every multi-currency wallet, including the ones that can't hold accounts at all
        portfolioSelectorController.isEnabled.value = { wallet, _ ->
            wallet.isAccountsSupported && wallet.walletId in eligible
        }
        navigation.activate(Unit)
    }

    /** In wallet-selection mode the chooser reports the picked wallet as its main account's id. */
    private fun observePickedWallet() {
        portfolioSelectorController.selectedAccounts
            .mapNotNull { accountIds -> accountIds.firstOrNull() }
            .mapNotNull(::walletOf)
            .onEach { walletId ->
                navigation.dismiss()
                selectedWalletId.value = walletId
            }
            .launchIn(scope)
    }

    private fun walletOf(accountId: AccountId): UserWalletId? = accountLists.value.values
        .firstOrNull { accounts -> accounts.accounts.any { it.accountId == accountId } }
        ?.userWalletId
}