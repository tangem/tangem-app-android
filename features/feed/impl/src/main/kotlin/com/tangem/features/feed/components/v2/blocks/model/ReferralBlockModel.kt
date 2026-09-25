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
 * navigating: silently when the programme is open to one wallet only, through the chooser when it is open
 * to several. Once every eligible wallet has joined, the card is dropped.
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

    /** Ticks when the statuses are worth reading again — see [onResume]. */
    private val statusRefreshes = MutableStateFlow(value = 0)

    /**
     * Wallets the programme is open to at all — the local test, which costs nothing. Compared by id,
     * so a balance refresh doesn't send the join-status requests again.
     */
    private val offeredWallets: StateFlow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filter { !it.isLocked && it.isReferralOffered() } }
        .distinctUntilChanged { old, new -> old.map(UserWallet::walletId) == new.map(UserWallet::walletId) }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /**
     * Which of the offered wallets have not joined yet, and whether that is known — reading the join
     * status costs a request per wallet, and the card is shown as a skeleton while they are in flight
     * rather than appearing only once the backend has answered.
     */
    private val invitation: StateFlow<Invitation> = offeredWallets
        .combine(statusRefreshes) { offered, _ -> offered }
        .transform { offered ->
            if (offered.isEmpty()) {
                emit(Invitation.Settled(invitable = emptyList()))
                return@transform
            }

            emit(Invitation.Pending)

            val invitable = offered.filterNot { it.hasJoinedReferral() }.map(UserWallet::walletId)
            emit(Invitation.Settled(invitable = invitable))
        }
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = Invitation.Settled(invitable = emptyList()),
        )

    val isVisible: StateFlow<Boolean> = invitation
        .map { state ->
            when (state) {
                Invitation.Pending -> true
                is Invitation.Settled -> state.invitable.isNotEmpty()
            }
        }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = false)

    val isLoading: StateFlow<Boolean> = invitation
        .map { it is Invitation.Pending }
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

    /** Set while the screen this card leads to is up — the only window in which a status can change. */
    private var isReferralScreenOpen = false

    /**
     * Joining happens on the referral screen and reaches nothing this model listens to, so the statuses
     * are read again on the way back from it — and only from it, since no other screen can have changed them.
     */
    fun onResume() {
        if (!isReferralScreenOpen) return
        isReferralScreenOpen = false

        statusRefreshes.update { it + 1 }
    }

    fun onClick() {
        val invitable = (invitation.value as? Invitation.Settled)?.invitable.orEmpty()

        when {
            invitable.isEmpty() -> TangemLogger.i("Referral: no wallet left to invite; staying on the feed")
            offeredWallets.value.size == 1 -> openReferral(invitable.single())
            else -> showWalletSelector(invitable = invitable.toSet())
        }
    }

    private fun showWalletSelector(invitable: Set<UserWalletId>) {
        // The chooser lists every wallet; the ones that can't be invited stay disabled
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
        isReferralScreenOpen = true
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

    private sealed interface Invitation {

        data object Pending : Invitation

        data class Settled(val invitable: List<UserWalletId>) : Invitation
    }
}