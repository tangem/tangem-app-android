package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.slot.navigate
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.blocks.AddAccountBlockComponent
import com.tangem.features.feed.impl.R
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Account creation offered by the [AddAccountBlockComponent] shortcut.
 *
 * It is not bound to the selected wallet: it offers every wallet that can hold accounts, so a click
 * settles the wallet first — silently when only one qualifies, through the chooser when several do —
 * and only then asks which kind of account to create. A wallet that can't hold accounts at all — a
 * single-currency card — hides the block rather than let it lead nowhere.
 */
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class AddAccountBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
    userWalletsListRepository: UserWalletsListRepository,
    multiAccountListSupplier: MultiAccountListSupplier,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model() {

    /** Wallets that can hold accounts at all; a locked one is excluded — its accounts can't be read. */
    private val accountCapableWallets: StateFlow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filter { it.isAccountsSupported && !it.isLocked } }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private val accountLists: StateFlow<List<AccountList>> = multiAccountListSupplier()
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /**
     * Wallets a click can actually act on: account-capable and with their account list already known.
     *
     * A missing account list is not an empty one — the supplier combines every wallet, so it stays silent
     * until the slowest one reports, and falls back to an empty list when it fails. Either way the cap is
     * unknown, and the shortcut has nothing to offer.
     */
    private val actionableWallets: StateFlow<List<Pair<UserWallet, AccountList>>> =
        combine(accountCapableWallets, accountLists) { wallets, lists ->
            wallets.mapNotNull { wallet ->
                lists.firstOrNull { it.userWalletId == wallet.walletId }?.let { wallet to it }
            }
        }.stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** Hidden while no wallet can be acted on, so the card is never offered as a tap that does nothing. */
    val isVisible: StateFlow<Boolean> = actionableWallets
        .map { it.isNotEmpty() }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = false)

    /** Read by the block's component, which owns the slot the sheets are shown in. */
    val bottomSheetNavigation = SlotNavigation<AddAccountBlockComponent.BottomSheetConfig>()

    /** Lazy: reading every wallet's balances is only worth it once the chooser is actually opened. */
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = ::dismissWalletSelector
        override val onBack: () -> Unit = ::dismissWalletSelector
    }

    init {
        observeChosenWallet()
    }

    /**
     * The chooser's modal keeps collapsing after a wallet is picked and reports that as a dismissal, by then
     * the account type sheet already occupies the slot and must survive it.
     */
    private fun dismissWalletSelector() {
        bottomSheetNavigation.navigate { config ->
            config.takeUnless { it is AddAccountBlockComponent.BottomSheetConfig.WalletSelector }
        }
    }

    fun onClick() {
        val knownWallets = actionableWallets.value

        // Visibility already tracks this; a list emptied between render and tap must not claim a limit
        if (knownWallets.isEmpty()) return

        val walletsWithRoom = knownWallets.filter { (_, accounts) -> accounts.hasRoomForAnyAccount() }
        when (walletsWithRoom.size) {
            0 -> messageSender.send(accountLimitReachedDialog())
            1 -> openAccountTypeChoice(walletsWithRoom.single().first.walletId)
            else -> openWalletChooser(eligibleWallets = walletsWithRoom.mapTo(mutableSetOf()) { it.first.walletId })
        }
    }

    /** A wallet at the crypto cap is still worth offering while it can take a joint account. */
    private fun AccountList.hasRoomForAnyAccount(): Boolean = canAddMoreCryptoAccounts ||
        jointAccountFeatureToggles.isJointAccountEnabled && canAddMoreJointAccounts

    private fun openWalletChooser(eligibleWallets: Set<UserWalletId>) {
        // The chooser lists every multi-currency wallet; the ones already at the cap stay disabled
        portfolioSelectorController.isEnabled.value = { wallet, _ -> wallet.walletId in eligibleWallets }
        bottomSheetNavigation.activate(AddAccountBlockComponent.BottomSheetConfig.WalletSelector)
    }

    /**
     * Which kind of account to create. With joint accounts not shipped there is nothing to choose, so
     * the sheet is skipped entirely and crypto creation opens straight away.
     *
     * A kind the wallet has no room for is still offered and explains itself in a dialog, as it does in
     * wallet settings — the caps are per kind, and silently dropping an option reads as a bug.
     */
    private fun openAccountTypeChoice(walletId: UserWalletId) {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) {
            bottomSheetNavigation.dismiss()
            openCryptoAccountCreation(walletId)
            return
        }

        val accounts = accountLists.value.firstOrNull { it.userWalletId == walletId } ?: return
        bottomSheetNavigation.activate(
            AddAccountBlockComponent.BottomSheetConfig.AccountType(
                userWalletId = walletId,
                isCryptoAccountLimitReached = !accounts.canAddMoreCryptoAccounts,
                isJointAccountLimitReached = !accounts.canAddMoreJointAccounts,
            ),
        )
    }

    fun onCryptoAccountClick(config: AddAccountBlockComponent.BottomSheetConfig.AccountType) {
        bottomSheetNavigation.dismiss()
        if (config.isCryptoAccountLimitReached) {
            messageSender.send(accountLimitReachedDialog())
        } else {
            openCryptoAccountCreation(config.userWalletId)
        }
    }

    fun onJointAccountClick(config: AddAccountBlockComponent.BottomSheetConfig.AccountType) {
        bottomSheetNavigation.dismiss()
        if (config.isJointAccountLimitReached) {
            messageSender.send(jointAccountLimitReachedDialog())
        } else {
            router.push(AppRoute.JointAccountCreation(userWalletId = config.userWalletId))
        }
    }

    /**
     * In wallet-selection mode the chooser reports the picked wallet as its main account's id.
     *
     * Deliberately not distinct: the controller re-emits an unchanged selection so that picking the
     * same wallet a second time opens creation again.
     */
    private fun observeChosenWallet() {
        portfolioSelectorController.selectedAccounts
            .mapNotNull { accountIds -> accountIds.firstOrNull() }
            .mapNotNull(::walletOf)
            .onEach { walletId -> openAccountTypeChoice(walletId) }
            .launchIn(modelScope)
    }

    private fun walletOf(accountId: AccountId): UserWalletId? = accountLists.value
        .firstOrNull { accounts -> accounts.accounts.any { it.accountId == accountId } }
        ?.userWalletId

    private fun openCryptoAccountCreation(walletId: UserWalletId) {
        router.push(AppRoute.CreateAccount(userWalletId = walletId))
    }

    private fun accountLimitReachedDialog(): DialogMessage =
        limitReachedDialog(limit = AccountList.MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT)

    private fun jointAccountLimitReachedDialog(): DialogMessage =
        limitReachedDialog(limit = AccountList.MAX_JOINT_ACCOUNTS_COUNT)

    private fun limitReachedDialog(limit: Int): DialogMessage = DialogMessage(
        title = resourceReference(R.string.account_add_limit_dialog_title),
        message = resourceReference(
            id = R.string.account_add_limit_dialog_description,
            formatArgs = wrappedList(limit.toString()),
        ),
        firstActionBuilder = {
            EventMessageAction(title = resourceReference(R.string.common_got_it), onClick = {})
        },
    )
}