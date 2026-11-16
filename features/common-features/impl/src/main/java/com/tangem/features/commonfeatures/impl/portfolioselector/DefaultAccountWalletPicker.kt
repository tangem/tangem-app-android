package com.tangem.features.commonfeatures.impl.portfolioselector

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Pick
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Resolution
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.commonfeatures.impl.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultAccountWalletPicker @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
    private val portfolioSelectorController: PortfolioSelectorController,
) : AccountWalletPicker {

    override val navigation = SlotNavigation<Unit>()

    private val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true), scope = scope)
    }

    private val bottomSheetCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { navigation.dismiss() }
        override val onBack: () -> Unit = { navigation.dismiss() }
    }

    override val selectorParams: PortfolioSelectorComponent.Params
        get() = PortfolioSelectorComponent.Params(
            portfolioFetcher = portfolioFetcher,
            controller = portfolioSelectorController,
            bsCallback = bottomSheetCallback,
            settings = PortfolioSelectorComponent.Settings(
                isWalletSelectionOnly = true,
                isDs3Modal = true,
                areLockedWalletsInline = true,
            ),
        )

    override val wallets: StateFlow<List<UserWallet>?> by lazy {
        getWalletsUseCase()
            .map { wallets -> wallets.filter { it.isAccountsSupported && !it.isLocked } }
            .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)
    }

    private val accountLists: StateFlow<Map<UserWalletId, AccountList>> by lazy {
        wallets
            .filterNotNull()
            .map { wallets -> wallets.map(UserWallet::walletId) }
            .distinctUntilChanged()
            .flatMapLatest { walletIds ->
                walletIds.map(singleAccountListSupplier::invoke)
                    .merge()
                    .scan(emptyMap<UserWalletId, AccountList>()) { lists, list -> lists + (list.userWalletId to list) }
            }
            .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyMap())
    }

    private var chooserFullWalletIds: Set<UserWalletId> = emptySet()

    /** In wallet-selection mode the sheet reports the picked wallet as its main account's id. */
    override val picks: Flow<Pick> = portfolioSelectorController.selectedAccounts
        .mapNotNull { accountIds -> accountIds.firstOrNull() }
        .mapNotNull(::walletOf)
        .map { walletId ->
            if (walletId in chooserFullWalletIds) {
                Pick.Full
            } else {
                navigation.dismiss()
                Pick.Wallet(walletId)
            }
        }

    override suspend fun resolve(hasRoom: (AccountList) -> Boolean): Resolution {
        val (wallets, lists) = withTimeoutOrNull(5.seconds) {
            combine(wallets.filterNotNull(), accountLists, ::Pair)
                .first { (wallets, lists) -> wallets.all { it.walletId in lists } }
        } ?: (wallets.value.orEmpty() to accountLists.value)

        val knownLists = wallets.mapNotNull { lists[it.walletId] }
        val single = knownLists.singleOrNull()
        return when {
            knownLists.isEmpty() -> Resolution.Unavailable
            single != null && hasRoom(single) -> Resolution.Single(single.userWalletId)
            single != null -> Resolution.LimitReached
            else -> Resolution.Choose(
                fullWalletIds = knownLists.filterNot(hasRoom).mapTo(mutableSetOf(), AccountList::userWalletId),
            )
        }
    }

    override fun openChooser(fullWalletIds: Set<UserWalletId>) {
        chooserFullWalletIds = fullWalletIds
        val knownWalletIds = accountLists.value.keys
        // The sheet lists every multi-currency wallet; one that can't hold accounts or whose list never loaded
        // stays disabled
        portfolioSelectorController.isEnabled.value = { wallet, _ ->
            wallet.isAccountsSupported && wallet.walletId in knownWalletIds
        }
        portfolioSelectorController.walletBadge.value = { wallet ->
            resourceReference(R.string.account_add_limit_badge).takeIf { wallet.walletId in fullWalletIds }
        }
        navigation.activate(Unit)
    }

    private fun walletOf(accountId: AccountId): UserWalletId? = accountLists.value.values
        .firstOrNull { accounts -> accounts.accounts.any { it.accountId == accountId } }
        ?.userWalletId

    @AssistedFactory
    interface Factory : AccountWalletPicker.Factory {
        override fun create(scope: CoroutineScope): DefaultAccountWalletPicker
    }
}