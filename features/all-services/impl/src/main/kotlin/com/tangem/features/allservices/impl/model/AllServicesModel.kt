package com.tangem.features.allservices.impl.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.allservices.impl.ui.state.AllServicesUM
import com.tangem.features.introduction.AddWalletComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class AllServicesModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model(), AddWalletComponent.ModelCallbacks {

    val uiState: StateFlow<AllServicesUM>
        field = MutableStateFlow(
            AllServicesUM(
                onReferralClick = ::onReferralClick,
                onBuyTangemClick = ::onBuyTangemClick,
                onCloseClick = router::pop,
            ),
        )

    val bottomSheetNavigation = SlotNavigation<WalletSelectorConfig>()

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

    private fun onReferralClick() {
        // The chooser lists every wallet; the ones outside the programme stay disabled
        portfolioSelectorController.isEnabled.value = { wallet, _ -> wallet.isReferralOffered() }
        portfolioSelectorController.selectAccount(accountId = null)
        observeWalletSelection()
        bottomSheetNavigation.activate(WalletSelectorConfig)
    }

    private fun observeWalletSelection() {
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first?.walletId }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::openReferral)
            .launchIn(modelScope)
            .saveIn(walletSelectionJob)
    }

    private fun openReferral(walletId: UserWalletId) {
        bottomSheetNavigation.dismiss()
        router.push(AppRoute.ReferralProgram(userWalletId = walletId))
        walletSelectionJob.cancel()
    }

    override fun onAddCardClick() {
        router.push(AppRoute.CreateHardwareWallet(source = AnalyticsParam.ScreensSources.AddNew))
    }

    private fun onBuyTangemClick() {
        modelScope.launch {
            val link = generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Settings)
            urlOpener.openUrl(link)
        }
    }

    /** Same eligibility as the wallet-settings entry, minus demo cards, where the programme is unavailable. */
    private fun UserWallet.isReferralOffered(): Boolean = when (this) {
        is UserWallet.Cold -> cardTypesResolver.isTangemWallet() && !isDemoCardUseCase(cardId)
        is UserWallet.Hot -> true
    }

    data object WalletSelectorConfig
}