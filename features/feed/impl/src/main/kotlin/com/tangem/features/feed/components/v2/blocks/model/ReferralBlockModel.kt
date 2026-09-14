package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.referral.ReferralRepository
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.blocks.ReferralBlockComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Which wallets the referral invitation is worth making to, and which one a tap opens it for.
 *
 * The programme is per wallet, and so is the screen the card leads to, so the wallet is settled before
 * navigating: silently when only one wallet can still be invited, through the chooser when several can.
 * Once every eligible wallet has joined, the card is dropped.
 */
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class ReferralBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val referralRepository: ReferralRepository,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    userWalletsListRepository: UserWalletsListRepository,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model() {

    /**
     * Wallets that can still be invited.
     *
     * Reading the join status costs a request per wallet, so the local test — programme open to this
     * wallet, wallet unlocked — runs first and only its survivors are asked. Compared by id, so a
     * balance refresh doesn't send the requests again.
     */
    private val invitableWallets: StateFlow<List<UserWalletId>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filter { !it.isLocked && it.isReferralOffered() } }
        .distinctUntilChanged { old, new -> old.map(UserWallet::walletId) == new.map(UserWallet::walletId) }
        .map { offered -> offered.filterNot { it.hasJoinedReferral() }.map(UserWallet::walletId) }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    val isVisible: StateFlow<Boolean> = invitableWallets
        .map { it.isNotEmpty() }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = false)

    /** Read by the block's component, which owns the slot the chooser is shown in. */
    val bottomSheetNavigation = SlotNavigation<ReferralBlockComponent.WalletSelectorConfig>()

    /** Lazy: reading every wallet's balances is only worth it once the chooser is actually opened. */
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = bottomSheetNavigation::dismiss
        override val onBack: () -> Unit = bottomSheetNavigation::dismiss
    }

    private val walletSelectionJob = JobHolder()

    fun onClick() {
        val wallets = invitableWallets.value
        when (wallets.size) {
            0 -> TangemLogger.i("Referral: no wallet left to invite; staying on the feed")
            1 -> openReferral(wallets.single())
            else -> showWalletSelector(invitable = wallets.toSet())
        }
    }

    private fun showWalletSelector(invitable: Set<UserWalletId>) {
        // The chooser lists every wallet; the ones the programme isn't open to stay disabled
        portfolioSelectorController.isEnabled.value = { wallet, _ -> wallet.walletId in invitable }
        portfolioSelectorController.selectAccount(accountId = null)
        observeWalletSelection()
        bottomSheetNavigation.activate(ReferralBlockComponent.WalletSelectorConfig)
    }

    private fun observeWalletSelection() {
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first?.walletId }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { walletId ->
                bottomSheetNavigation.dismiss()
                openReferral(walletId)
            }
            .launchIn(modelScope)
            .saveIn(walletSelectionJob)
    }

    private fun openReferral(walletId: UserWalletId) {
        router.push(AppRoute.ReferralProgram(userWalletId = walletId))
    }

    /**
     * Whether the programme is open to this wallet at all. Mirrors the wallet-settings entry, plus
     * demo cards — there the row survives and explains itself in a dialog, but a promo card that can
     * only ever apologise is better left out.
     */
    private fun UserWallet.isReferralOffered(): Boolean = when (this) {
        is UserWallet.Cold -> cardTypesResolver.isTangemWallet() && !isDemoCardUseCase(cardId)
        is UserWallet.Hot -> true
    }

    /**
     * A status we failed to read counts as "not joined": the invite is worth more than the risk of
     * hiding it from someone eligible.
     */
    private suspend fun UserWallet.hasJoinedReferral(): Boolean = runSuspendCatching {
        referralRepository.getReferralStatus(userWalletId = walletId.stringValue).isActive
    }
        .onFailure { TangemLogger.e("Referral status unavailable, keeping the invite visible", it) }
        .getOrDefault(false)
}